# The Train

Transactional file publishing tool over HTTP.

Inspired by The Train from The Magic Roundabout.

## Basics

FIve APIs are available to control file publishing:

 * POST to `/begin` to begin a transaction and get a `transactionId`.
 * POST a file to `/publish` to add a file to the transaction, specifying `transactionId` and `uri` parameters in the url.
 * POST to `/commit` or `/rollback`, specifying a `transactionId` parameter in the url to end a transaction.
 * GET `/transaction`, specifying a `transactionId` parameter in the url to get the details of a transaction.
 
By default the 

## Encryption

If you wish to encrypt transferred files until a transaction is committed, specify an `encryptionPassword` parameter in the url. This will be used to trigger AES encryption.

HTTP uploads stored as temp files are encrypted by default, using classes based on the implementatios provided by Apache Commons Fileupload:

 * `EncryptedFileItemFactory`
 * `EncryptedFileItem`
 * `EncryptedDeferredOutputStream`

The `EncryptedFileItemFactory` class ensures that any data written to disk as temp files are encrypted using a random AES key.

File upload encryption is implicit and requires no additional effort or adjustment.