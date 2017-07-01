/*
 * Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package models.services

import com.google.inject.{ ImplementedBy, Inject }
import models.{KeywordAggregation, KeywordNetwork}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality

import scala.collection.mutable.ListBuffer
// added
import scalikejdbc.NamedDB
// scalastyle:off
import scala.collection.JavaConversions._
// scalastyle:on
import models.{Aggregation, Facets, MetaDataBucket, KeyTerm, NodeBucket, KeywordRelationship}
import util.es.ESRequestUtils

/**
 * Defines common method for creating and extending co-occurrence networks given a search query.
 *
 * The trait is implemented by [[models.services.ESKeywordNetworkService]].
 */
@ImplementedBy(classOf[ESKeywordNetworkService])
trait KeywordNetworkService {

  /**
   * Returns a co-occurrence network matching the given search query.
   *
   * @param facets the search query.
   * @param nodeFraction a map linking from entity types to the number of nodes to request for each type.
   * @param exclude a list of entity ids that should be excluded from the result. The result will contain no [[models.NodeBucket]]
   * associated with one of the ids given in this list.
   * @param index the data source index or database name to query.
   * @return a [[models.KeywordNetwork]] consisting of the nodes and relationships of the created co-occurrence network.
   */
  def createNetworkKeyword(facets: Facets, nodeFraction: Map[String, Int], exclude: List[String])(index: String): KeywordNetwork

  /**
   * Adds new nodes to the current network matching the given search query.
   *
   * The method induces relationships and nodes for the given entitiesToAdd considering the already present network i.e.
   * it induces relationships between the new nodes and the current network and between the new nodes. It does not
   * provide nodes for the current network nor relationships between those nodes.
   *
    * @param facets        the search query.
   * @param currentNetwork the keyword terms of the current network.
    * @param nodes         new keywords to be added to the network.
    * @param index         the data source index or database name to query.
   * @return a [[models.KeywordNetwork]] consisting of the nodes and relationships of the created co-occurrence network.
   */
  def induceNetworkKeyword(facets: Facets, currentNetwork: List[String], nodes: List[String])(index: String): KeywordNetwork

  /**
   * Returns entities co-occurring with the given entity matching the search query.
   *
   * @param facets the search query.
   * @param entityId the entity id.
   * @param size the number of neighbors to fetch.
   * @param exclude a list of entity ids that should be excluded from the result. The result will contain no [[models.NodeBucket]]
   * associated with one of the ids given in this list.
   * @param index the data source index or database name to query.
   * @return a list of [[models.NodeBucket]] co-occurring with the given entity.
   */
  def getNeighborsKeyword(facets: Facets, entityId: Long, size: Int, exclude: List[Long])(index: String): List[NodeBucket]

  /**
   * Accumulates the number of entities that fall in a certain entity type and co-occur with the given entity.
   *
   * The result will contain ''n'' different buckets with ''n'' representing the distinct entity types for the underlying collection.
   *
   * @param facets the search query.
   * @param entityId the entity id.
   * @param index the data source index or database name to query.
   * @return a map linking from the unique entity type to the number of neighbors of that type.
   */
  def getNeighborCountsPerTypeKeyword(facets: Facets, entityId: Long)(index: String): Map[String, Int]

  /**
   * @param nodes the entities
   */
  def setGraphNodes(nodes: List[NodeBucket]): Unit

  /**
   * @param facets   the search query.
   * @param entities the entities that should occur in the document.
   * @param numTerms the amount of keywords.
   * @param index    the data source index to query.
   * @return a list of [[models.KeyTerm]] representing all important terms
   */
  def getKeywordsForEntities(facets: Facets, entities: List[NodeBucket], numTerms: Int)(index: String): List[KeyTerm]
}

/**
 * Implementation of [[models.services.KeywordNetworkService]] using an elasticsearch index as backend.
 *
 * @param clientService    the elasticsearch client.
 * @param aggregateService the aggregation service.
 * @param entityService    the entity service.
 * @param utils            common helper to issue elasticsearch queries.
 * @param networkService  the network service
 */
class ESKeywordNetworkService @Inject() (
    clientService: SearchClientService,
    aggregateService: AggregateService,
    entityService: EntityService,
    utils: ESRequestUtils,
    networkService: NetworkService
) extends KeywordNetworkService {

  private val db = (index: String) => NamedDB(Symbol(index))

  var graphNodes: List[NodeBucket] = List[NodeBucket]()

  /** @inheritdoc */
  //noinspection ScalaStyle
  override def createNetworkKeyword(
    facets: Facets,
    nodeFraction: Map[String, Int],
    exclude: List[String]
  )(index: String): KeywordNetwork = {

    /*
    val rels: List[Relationship] = List(
      Relationship(1, 2, 2),
      Relationship(2, 3, 4),
      Relationship(3, 4, 5),
      Relationship(5, 6, 6),
      Relationship(6, 7, 7),
      Relationship(7, 8, 8),
      Relationship(8, 9, 8),
      Relationship(9, 1, 9),
      Relationship(3, 2, 4),
      Relationship(4, 2, 10),
      Relationship(5, 2, 13),
      Relationship(0, 4, 14),
      Relationship(10, 3, 15),
      Relationship(5, 7, 11),
      Relationship(6, 2, 40)
    )
    */

    val keywords = getKeywordsForEntities(facets, networkService.getGraphEntitites(), 10)(index)
    // TODO Relationships

    KeywordNetwork(keywords, induceRelationshipsKeyword(facets, for (keyword <- keywords) yield keyword.term, index))
  }

  /** @inheritdoc */
  private def induceRelationshipsKeyword(facets: Facets, nodes: List[String], index: String): List[KeywordRelationship] = {
    val visitedList = ListBuffer[String]()
    val rels = nodes.flatMap { source =>
      visitedList.add(source)
      val rest = nodes.filter(!visitedList.contains(_))
      rest.flatMap { dest => getRelationshipKeyword(facets, source, dest, index) }
    }
    rels
  }

  /** @inheritdoc */
  private def getRelationshipKeyword(facets: Facets, source: String, dest: String, index: String): Option[KeywordRelationship] = {
    val t = List(source, dest)
    // val agg = aggregateService.aggregateEntities(facets.withEntities(t), 2, t, Nil)(index)
    val agg = aggregateService.keywordAggregate(facets, utils.keywordsField._1, 2, t, Nil)(index)
    agg match {
      // No edge between both since their frequency is zero
      case KeywordAggregation(_, KeyTerm(nodeA, 0) :: KeyTerm(nodeB, 0) :: Nil) =>
        None
      case KeywordAggregation(_, KeyTerm(nodeA, freqA) :: KeyTerm(nodeB, freqB) :: Nil) =>
        // freqA and freqB are the same since we query for docs containing both
        Some(KeywordRelationship(nodeA, nodeB, freqA))
      case _ => None
    }
  }

  /** @inheritdoc */
  //noinspection ScalaStyle
  override def induceNetworkKeyword(facets: Facets, currentNetwork: List[String], nodes: List[String])(index: String): KeywordNetwork = {
    // Fetch relationships between new nodes
    val inBetweenRels = induceRelationshipsKeyword(facets, nodes, index)
    // Fetch relationships between new nodes and current network
    val connectingRels = nodes.flatMap { source =>
      currentNetwork.flatMap { dest => getRelationshipKeyword(facets, source, dest, index) }
    }

    val intvalue: Int = 20
    val keywords: Option[Int] = Option(intvalue)
    KeywordNetwork(getKeywordsForEntities(facets, networkService.getGraphEntitites(), 20)(index), inBetweenRels ++ connectingRels)

  }

  override def setGraphNodes(nodes: List[NodeBucket]): Unit = {
    this.graphNodes = nodes
  }

  /** @inheritdoc */
  override def getNeighborsKeyword(facets: Facets, entityId: Long, size: Int, exclude: List[Long])(index: String): List[NodeBucket] = {
    val res = aggregateService.aggregateEntities(facets.withEntities(List(entityId)), size, List(), exclude)(index)
    res.buckets.collect { case a @ NodeBucket(_, _) => a }
  }

  /** @inheritdoc */
  override def getNeighborCountsPerTypeKeyword(facets: Facets, entityId: Long)(index: String): Map[String, Int] = {
    // Add entity id as entities filter in order to receive documents where both co-occur
    val neighborFacets = facets.withEntities(List(entityId))
    val res = cardinalityAggregateKeyword(neighborFacets, 0, index)
    res.buckets.collect { case MetaDataBucket(term, count) => (term, count.toInt) }.toMap
  }

  // TODO: Maybe move to aggregateService
  private def cardinalityAggregateKeyword(facets: Facets, documentSize: Int, index: String): Aggregation = {
    val requestBuilder = utils.createSearchRequest(facets, documentSize, index, clientService)
    // Add neighbor aggregation for each NE type
    val types = entityService.getTypes()(index).keys
    types.foreach { t =>
      val aggregation = AggregationBuilders
        .cardinality(t)
        .field(utils.convertEntityTypeToField(t))

      requestBuilder.addAggregation(aggregation)
    }
    val response = utils.executeRequest(requestBuilder)
    // Parse result
    val buckets = types.map { t =>
      val agg: Cardinality = response.getAggregations.get(t)
      MetaDataBucket(t, agg.getValue)
    }.toList

    Aggregation("neighbors", buckets)
  }

  /** @inheritdoc */
  def getKeywordsForEntities(facets: Facets, entities: List[NodeBucket], numTerms: Int)(index: String): List[KeyTerm] = {
    // Only consider documents where the entities occur
    val res = aggregateService.aggregateKeywords(facets.withEntities(for (entity <- entities) yield entity.id), numTerms, Nil, Nil)(index)
    res.buckets.collect { case MetaDataBucket(term, count) => KeyTerm(term, count.toInt) }
  }
}