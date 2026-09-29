package com.baselalhabib.personalcontext.core.connectors

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import com.baselalhabib.personalcontext.core.entities.ContactEntity
import com.baselalhabib.personalcontext.core.permissions.PermissionsHelper
import java.util.UUID

/**
 * Connector implementation for contact context entities.
 * Supports both manual/staged contacts and querying Android Contacts provider when [context] is supplied.
 */
class ContactConnector(
    private val context: Context? = null
) : Connector<ContactEntity> {

    override val id: String = "contact_connector"

    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.READ_CONTACTS
    )

    private val pendingContacts = mutableListOf<ContactEntity>()

    /**
     * Creates and stages a new [ContactEntity] into the connector's pending queue.
     */
    fun addContact(
        name: String,
        phoneNumber: String? = null,
        email: String? = null,
        lookupKey: String? = null,
        id: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis(),
        source: String = "contacts"
    ): ContactEntity {
        val contact = ContactEntity(
            id = id,
            timestamp = timestamp,
            source = source,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            lookupKey = lookupKey
        )
        synchronized(pendingContacts) {
            pendingContacts.add(contact)
        }
        return contact
    }

    /**
     * Stages an existing [ContactEntity] into the connector's pending queue.
     */
    fun addContact(contact: ContactEntity) {
        synchronized(pendingContacts) {
            pendingContacts.add(contact)
        }
    }

    /**
     * Collects contacts from both the pending queue and the device's Contacts provider (if [context] is present
     * and permission granted). Clears collected pending contacts.
     */
    override suspend fun collect(sinceTimestamp: Long?): List<ContactEntity> {
        val results = mutableListOf<ContactEntity>()

        synchronized(pendingContacts) {
            val collectedPending = if (sinceTimestamp != null) {
                pendingContacts.filter { it.timestamp >= sinceTimestamp }
            } else {
                pendingContacts.toList()
            }
            results.addAll(collectedPending)
            pendingContacts.removeAll(collectedPending)
        }

        context?.let { ctx ->
            if (PermissionsHelper.isPermissionGranted(ctx, Manifest.permission.READ_CONTACTS)) {
                results.addAll(queryContactsProvider(ctx, sinceTimestamp))
            }
        }

        return results
    }

    private fun queryContactsProvider(ctx: Context, sinceTimestamp: Long?): List<ContactEntity> {
        val contacts = mutableListOf<ContactEntity>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
        )

        val selection = if (sinceTimestamp != null) {
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP} >= ?"
        } else null

        val selectionArgs = if (sinceTimestamp != null) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        try {
            ctx.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val updatedColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP)
                val lookupColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

                while (cursor.moveToNext()) {
                    val contactId = if (idColumn != -1) cursor.getString(idColumn) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "Unknown" else "Unknown"
                    val number = if (numberColumn != -1) cursor.getString(numberColumn) else null
                    val lastUpdated = if (updatedColumn != -1) cursor.getLong(updatedColumn) else System.currentTimeMillis()
                    val lookupKey = if (lookupColumn != -1) cursor.getString(lookupColumn) else null

                    contacts.add(
                        ContactEntity(
                            id = "contact_$contactId",
                            timestamp = if (lastUpdated > 0) lastUpdated else System.currentTimeMillis(),
                            source = "contacts",
                            name = name,
                            phoneNumber = number,
                            lookupKey = lookupKey
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Return collected so far if query fails
        }

        return contacts
    }

    /**
     * Returns a snapshot of all currently pending contacts without clearing them.
     */
    fun getPendingContacts(): List<ContactEntity> {
        synchronized(pendingContacts) {
            return pendingContacts.toList()
        }
    }

    /**
     * Clears all pending contacts.
     */
    fun clearPendingContacts() {
        synchronized(pendingContacts) {
            pendingContacts.clear()
        }
    }
}
