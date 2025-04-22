package com.justxraf.skyblockevents.util

import com.justxdude.skyblockapi.SkyblockAPI
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.MongoCollection
import org.bson.Document
import java.util.concurrent.CompletableFuture.supplyAsync

object DatabaseUtil {
    private val gson = SkyblockAPI.instance.database.gson
    fun <K, V> Map<K, V>.saveToDatabase(collection: MongoCollection<Document>) {
        if(this.isEmpty()) return // Found: com.mongodb.client.MongoCollection<Document!>!
        supplyAsync {
            val bulkWriteRequests = this.map { it.key to it.value }.map { (id, finishedEvent) ->
                val json = gson.toJson(finishedEvent)
                val document = Document.parse(json)
                val filter = Document("_id", id)
                UpdateOneModel<Document>(filter, Document("\$set", document), UpdateOptions().upsert(true))
            }
            collection.bulkWrite(bulkWriteRequests)
        }.exceptionally { ex ->
            println("Exception in async task: ${ex.message}")
            ex.printStackTrace()
            null
        }
    }
}