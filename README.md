# The Train

Transactional file publishing tool over HTTP, inspired by The Train from The Magic Roundabout.

Runs an embedded Jetty server that accepts files in parallel over HTTP and commits them to a local directory, backing up replaced files as necessary and providing optional encryption.

## Basics

FIve APIs are available to control file publishing:

 * POST to `/begin` to begin a transaction and get a `transactionId`.
 * POST the files you want to publish to `/publish` to add them to the transaction, specifying `transactionId` and `uri` (destination path within the target directory) parameters in the url.
 * POST to `/commit` or `/rollback`, specifying a `transactionId` parameter in the url to end a transaction.
 * GET `/transaction`, specifying a `transactionId` parameter in the url to get the details of a transaction.
 
By default the publisher will operate on temp directories. It prints out console messages about the configuration variables you can use to set up directories in production.

## Encryption

If you wish to encrypt transferred files until a transaction is committed, specify an `encryptionPassword` parameter in the url. This will be used to trigger AES encryption.

HTTP uploads stored as temp files are encrypted by default, using classes based on the implementatios provided by Apache Commons Fileupload:

 * `EncryptedFileItemFactory`
 * `EncryptedFileItem`
 * `EncryptedDeferredOutputStream`

The `EncryptedFileItemFactory` class ensures that any data written to disk as temp files are encrypted using a random AES key.

File upload encryption is implicit and requires no additional effort or adjustment.