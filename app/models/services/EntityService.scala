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

import com.google.inject.ImplementedBy
import models.KeyTerm

import scala.collection.mutable.ListBuffer
// scalastyle:off
import scalikejdbc._
// scalastyle:on
import models.{ Entity, Fragment }

/**
 * Defines common data access methods for retrieving and manipulating [[models.Entity]].
 *
 * The trait is implemented by [[models.services.DBDocumentService]].
 */
@ImplementedBy(classOf[DBEntityService])
trait EntityService {

  /**
   * Returns a list of [[models.Entity]] matching the given entity ids.
   *
   * @param ids a list of entity ids.
   * @param index the data source index or database name to query.
   * @return a list of [[models.Entity]] corresponding to the given ids or [[scala.Nil]] if no
   * matching entity is found.
   */
  def getByIds(ids: List[Long])(index: String): List[Entity]

  /**
   * Returns a record of [[models.Entity]] matching the given entity name and type.
   *
   * @param name entity name.
   * @param enType entity type
   * @param index the data source index or database name to query.
   * @return a list of [[models.Entity]] corresponding to the given name and type or [[scala.Nil]] if no
   * matching entity is found.
   */
  def getNameAndType(name: String, enType: String)(index: String): List[Entity]

  // added
  /*
  /**
   * Returns a list of [[models.KeyTerm]] matching the given keyword ids.
   *
   * @param ids a list of entity ids.
   * @param index the data source index or database name to query.
   * @return a list of [[models.KeyTerm]] corresponding to the given ids or [[scala.Nil]] if no
   * matching keyword is found.
   */
  def getKeywordByIds(ids: List[Long])(index: String): List[Entity]
  */

  /**
   * Marks the entities associated with the given ids as blacklisted.
   *
   * Blacklisted entities don't appear in any result set.
   *
   * @param ids the entity ids to blacklist.
   * @param index the data source index or database name to query.
   * @return ''true'', if all entities are successfully marked as blacklisted. ''False'' if at least one entity
   * is not correct marked.
   */
  def blacklist(ids: List[Long])(index: String): Boolean

  /**
   * Marks the keywords associated with the given ids as blacklisted.
   *
   * Blacklisted keywords don't appear in any result set.
   *
   * @param keyword the keyword to blacklist.
   * @param index   the data source index or database name to query.
   * @return ''true'', if all keywords are successfully marked as blacklisted. ''False'' if at least one keyword
   *         is not correct marked.
   */
  def blacklistKeyword(keyword: String)(index: String): Boolean

  /**
   * Marks the entity to whitelist.
   *
   * Whitelist new entity
   *
   * @param text the entity text to whitelist.
   * @param start the start of entity offset.
   * @param end the end of entity offset.
   * @param index the data source index or database name to query.
   * @param docId the identifier of document
   * @return ''true'', if all entities are successfully marked as blacklisted. ''False'' if at least one entity
   * is not correct marked.
   */
  def whitelist(text: String, start: Int, end: Int, enType: String, docId: BigInt)(index: String): Boolean

  /**
   * Marks the entity to whitelist.
   *
   * Whitelist new entity
   *
   * @param text the entity text to whitelist.
   * @param start the start of entity offset.
   * @param end the end of entity offset.
   * @param index the data source index or database name to query.
   * @param entId the identifier of entity
   * @param docId the identifier of document
   * @return ''true'', if all entities are successfully marked as blacklisted. ''False'' if at least one entity
   * is not correct marked.
   */
  def updateFrequency(text: String, start: Int, end: Int, enType: String, entId: BigInt, docId: BigInt)(index: String): Boolean

  /**
   * Removes the blacklisted mark from the entities associated with the given ids.
   *
   * After executing this operation, the respective entities do appear in result sets again.
   *
   * @param ids the entity ids to remove the blacklist mark from.
   * @param index the data source index or database name to query.
   * @return ''true'', if the blacklist mark is successfully removed from all entities. ''False'' if at least one blacklist
   * mark for an entity is not correct removed.
   */
  def undoBlacklist(ids: List[Long])(index: String): Boolean

  /**
   * Returns all blacklisted entities for the underlying collection.
   *
   * @param index the data source index or database name to query.
   * @return a list of [[models.Entity]], where each entity is marked as blacklisted.
   */
  def getBlacklisted()(index: String): List[Entity]

  /**
   * Returns all blacklisted keywords for the underlying collection.
   *
   * @param index the data source index or database name to query.
   * @return a list of String, where each keyterm is marked as blacklisted.
   */
  def getBlacklistedKeywords()(index: String): List[String]

  /**
   * Merges multiple nodes in a given focal node.
   *
   * The duplicates don't appear in any result set anymore. Further, any entity-related search using the focal node as
   * instance also queries for its duplicates i.e. searching for "Angela Merkel" will also search for "Angela" or "Dr. Merkel".
   *
   * @param focalId the central entity id.
   * @param duplicates entity ids referring to similar textual mentions of the focal id.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean

  /**
   * Merges multiple keywords in a given focal node.
   *
   * @param focalId the central keyword.
   * @param duplicates keywords referring to similar textual mentions of the focal keyword.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def mergeKeywords(focalId: String, duplicates: List[String])(index: String): Boolean

  /**
   * Withdraws [[models.services.EntityService#merge]] for the given entity id.
   *
   * @param focalIds the central entity ids.
   * @param index the data source index or database name to query.
   * @return ''true'', if the removal was successful. ''False'' otherwise.
   */
  def undoMerge(focalIds: List[Long])(index: String): Boolean

  /**
   * Withdraws [[models.services.EntityService#merge]] for the given keyword.
   *
   * @param focalIds the central keywords.
   * @param index the data source index or database name to query.
   * @return ''true'', if the removal was successful. ''False'' otherwise.
   */
  def undoMergeKeywords(focalIds: List[String])(index: String): Boolean

  /**
   * Returns all merged entities for the underlying collection.
   *
   * @param index the data source index or database name to query.
   * @return a map linking from the focal entity to its duplicates.
   */
  def getMerged()(index: String): Map[Entity, List[Entity]]

  /**
   * Returns all merged keywords for the underlying collection.
   *
   * @param index the data source index or database name to query.
   * @return a map linking from the focal keyword to its duplicates.
   */
  def getMergedKeywords()(index: String): Map[String, List[String]]

  /**
   * Changes the name of the entity corresponding to the given entity id.
   *
   * @param id the entity id to change.
   * @param newName the new name to apply.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def changeName(id: Long, newName: String)(index: String): Boolean

  /**
   * Changes the type of the entity corresponding to the given entity id.
   *
   * @param id the entity id to change.
   * @param newType the new type to apply.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def changeType(id: Long, newType: String)(index: String): Boolean

  /**
   * Returns all entity occurrences for the given document including their position in the document.
   *
   * @param docId the document id.
   * @param index the data source index or database name to query.
   * @return a list of tuple consisting of an entity and its position in the document.
   */
  def getEntityFragments(docId: Long)(index: String): List[(Entity, Fragment)]

  /**
   * Returns all blacklisted entity occurrences for the given document including their position in the document.
   *
   * @param docId the document id.
   * @param index the data source index or database name to query.
   * @return a list of tuple consisting of an entity and its position in the document.
   */
  def getBlacklistFragments(docId: Long)(index: String): List[(Entity, Fragment)]

  /** Returns a map of distinct entity types linking to a unique id for the underlying collection. */
  def getTypes()(index: String): Map[String, Int]
}

/** Implementation of [[models.services.EntityService]] using a relational database. */
class DBEntityService extends EntityService {

  private val db = (index: String) => NamedDB(Symbol(index))

  /** @inheritdoc */
  override def getByIds(ids: List[Long])(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE id IN (${ids})
                AND NOT isblacklisted
          ORDER BY frequency DESC""".map(Entity(_)).list.apply()
  }

  /** @inheritdoc */
  override def getNameAndType(name: String, enType: String)(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE name IN (${name})
          AND type IN (${enType})
          AND NOT isblacklisted
          ORDER BY frequency DESC""".map(Entity(_)).list.apply()
  }

  /** @inheritdoc */
  override def blacklist(ids: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = TRUE WHERE id IN (${ids})".update().apply()
    entityCount == ids.sum
  }

  /** @inheritdoc*/
  override def blacklistKeyword(keyword: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val numResults = sql"SELECT term FROM blacklistedkeywords WHERE ${keyword} = term".map(rs => rs.string("term")).list().apply()
    val keywordType = "KEYWORD"

    if (numResults.length == 0) {
      sql"INSERT INTO blacklistedkeywords(term, type) values (${keyword}, ${keywordType})".update().apply()
    }
    true
  }

  /** @inheritdoc */
  override def whitelist(text: String, start: Int, end: Int, enType: String, docId: BigInt)(index: String): Boolean = db(index).localTx { implicit session =>
    sql"INSERT INTO entity (id, name, type, frequency) VALUES ((SELECT coalesce(max(id),0)+1 FROM entity), ${text}, ${enType}, 1)".update().apply()
    sql"DELETE FROM entityoffset WHERE docId=${docId} AND entitystart=${start} AND entityend=${end}".update().apply()
    sql"""INSERT INTO entityoffset (docid, entid, entitystart, entityend)
         VALUES (${docId}, (SELECT coalesce(max(id),0) FROM entity), ${start}, ${end})""".update().apply()
    true
  }

  /** @inheritdoc */
  override def updateFrequency(text: String, start: Int, end: Int, enType: String, entId: BigInt, docId: BigInt)(index: String): Boolean = db(index).localTx { implicit session =>
    sql"  UPDATE entity SET frequency = (SELECT coalesce(max(frequency),0)+1 FROM entity WHERE id=${entId}) WHERE id=${entId}".update().apply()
    sql"""INSERT INTO entityoffset (docid, entid, entitystart, entityend)
         VALUES (${docId}, ${entId}, ${start}, ${end})""".update().apply()
    true
  }

  /** @inheritdoc */
  override def undoBlacklist(ids: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = FALSE WHERE id IN (${ids})".update().apply()
    // Remove entity also from the duplicates list
    val duplicateCount = sql"DELETE FROM duplicates WHERE duplicateid IN (${ids})".update().apply()
    // Successful, if updates one entity
    entityCount == ids.sum && duplicateCount == ids.sum
  }

  /** @inheritdoc */
  override def getBlacklisted()(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE isblacklisted".map(Entity(_)).list.apply()
  }

  /** @inheritdoc */
  override def getBlacklistedKeywords()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT term FROM blacklistedkeywords".map(rs => rs.string("term")).list().apply()
  }

  /** @inheritdoc */
  override def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    // Keep track of the origin entities for the given duplicates
    val merged = duplicates.map { id =>
      sql"INSERT INTO duplicates VALUES (${id}, ${focalId})".update.apply()
      // Blacklist duplicates in order to prevent that they show up in any query
      blacklist(List(id))(index)
    }
    merged.length == duplicates.length && merged.forall(identity)
  }

  /** @inheritdoc */
  override def mergeKeywords(focalId: String, duplicates: List[String])(index: String): Boolean = db(index).localTx { implicit session =>
    // Keep track of the origin keywords for the given duplicates
    // TODO merge keywords and undo merging

    val merged = duplicates.map { keyword =>
      sql"INSERT INTO duplicateKeywords VALUES (${keyword}, ${focalId})".update.apply()
      // Blacklist duplicates in order to prevent that they show up in any query
      blacklistKeyword(keyword)(index)
    }
    merged.length == duplicates.length && merged.forall(identity)
    true
  }

  /** @inheritdoc */
  override def undoMerge(focalIds: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    // Remove blacklist flag from all duplicate entries with matching focalIds
    sql"""UPDATE entity
          SET isblacklisted = FALSE
          FROM duplicates
          WHERE duplicateid = id AND focalid IN (${focalIds})""".update().apply()

    sql"DELETE FROM duplicates WHERE focalid IN (${focalIds})".update().apply()
    // TODO
    true
  }

  /** @inheritdoc */
  override def undoMergeKeywords(focalIds: List[String])(index: String): Boolean = db(index).localTx { implicit session =>

    var origin = focalIds.head
    sql"DELETE FROM duplicatekeywords where ($origin) = focal".update().apply()

    for (focal <- focalIds) {
      if (focalIds.indexOf(focal) > 0) {
        sql"DELETE FROM blacklistedkeywords where term = ($focal)".update().apply()
      }
    }

    true
  }

  /** @inheritdoc */
  override def getMerged()(index: String): Map[Entity, List[Entity]] = db(index).readOnly { implicit session =>
    var duplicates = sql"""SELECT e1.id, e1.name, e1.type, e1.frequency,
                                  e2.id AS focalId, e2.name AS focalName, e2.type AS focalType, e2.frequency AS focalFreq
                           FROM duplicates AS d
                           INNER JOIN entity AS e1 ON e1.id = d.duplicateid
                           INNER JOIN entity AS e2 ON e2.id = d.focalid""".map { rs =>
      (Entity(rs), Entity(
        rs.long("focalId"),
        rs.string("focalName"),
        rs.string("focalType"),
        rs.int("focalFreq")
      ))
    }.list.apply()

    var duplicateKeywords = getMergedKeywords()(index)

    var result = duplicates.groupBy { case (_, focalEntity) => focalEntity }.mapValues(_.map(_._1))

    for (keyword <- duplicateKeywords) {
      val list = keyword._2.map { case x: String => Entity(-1, x, "KEYWORD", 1) }
      result += (Entity(-1, keyword._1, "KEYWORD", 1) -> list)
    }

    result
  }

  /** @inheritdoc */
  override def getMergedKeywords()(index: String): Map[String, List[String]] = db(index).readOnly { implicit session =>
    val tuples = sql"""SELECT * FROM duplicatekeywords""".map { rs => (rs.string("duplicate"), rs.string("focal")) }.list.apply()

    var keywordMap: Map[String, ListBuffer[String]] = Map()

    for (tuple <- tuples) {
      var addToList = true
      for (entry <- keywordMap) {
        if (tuple._2 == entry._1) {
          addToList = false
          if (!entry._2.contains(tuple._1)) {
            entry._2.append(tuple._1)
          }
        }
      }
      if (addToList) {
        keywordMap += (tuple._2 -> ListBuffer(tuple._1))
      }
    }
    var result: Map[String, List[String]] = Map()

    keywordMap.map { case (a, b) => result += (a -> b.toList) }

    result
  }

  /** @inheritdoc */
  override def changeName(id: Long, newName: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val count = sql"UPDATE entity SET name = ${newName} WHERE id = ${id}".update().apply()
    // Successful, if apply updates one row
    count == 1
  }

  /** @inheritdoc */
  override def changeType(id: Long, newType: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val count = sql"UPDATE entity SET type = ${newType.toString} WHERE id = ${id}".update().apply()
    count == 1
  }

  /** @inheritdoc */
  override def getEntityFragments(docId: Long)(index: String): List[(Entity, Fragment)] = {
    val fragments = db(index).readOnly { implicit session =>
      sql"""SELECT entid AS id, e.name, e.type, e.frequency, entitystart, entityend FROM entityoffset
          INNER JOIN entity AS e ON e.id = entid
          WHERE docid = ${docId}
          AND NOT e.isblacklisted
       """.map(rs => (Entity(rs), rs.int("entitystart"), rs.int("entityend"))).list.apply()
    }
    fragments.map { case (e, start, end) => (e, Fragment(start, end)) }
  }

  /** @inheritdoc */
  override def getBlacklistFragments(docId: Long)(index: String): List[(Entity, Fragment)] = {
    val fragments = db(index).readOnly { implicit session =>
      sql"""SELECT entid AS id, e.name, e.type, e.frequency, entitystart, entityend FROM entityoffset
          INNER JOIN entity AS e ON e.id = entid
          WHERE docid = ${docId}
          AND e.isblacklisted
       """.map(rs => (Entity(rs), rs.int("entitystart"), rs.int("entityend"))).list.apply()
    }
    fragments.map { case (e, start, end) => (e, Fragment(start, end)) }
  }

  /** @inheritdoc */
  override def getTypes()(index: String): Map[String, Int] = db(index).readOnly { implicit session =>
    val names = sql"SELECT DISTINCT type FROM entity WHERE NOT isblacklisted".map(rs => rs.string("type")).list.apply()
    names.zipWithIndex.toMap
  }
}
