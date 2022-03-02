package main

// Transaction is the main object used to communicate between Zebedee and The-Train
type Transaction struct {
	Id         string        `json:"id"`
	Status     string        `json:"status"`
	StartDate  string        `json:"startDate"`
	EndDate    string  	     `json:"endDate"`
	UriInfos   []string		 `json:"uriInfos"`
	UriDeletes []string 	 `json:"uriDeletes"`
	Errors     []string      `json:"errors"`
	Files      []string      `json:"files"`
}

// PostBeginResponse is the object returned from begin endpoint
type PostBeginResponse struct {
	Message     string 		 `json:"message"`
	Error       bool   		 `json:"error"`
	Transaction Transaction  `json:"transaction"`
}

// PostCommitManifestRequest is the body of the request for CommitManifest endpoint
type PostCommitManifestRequest struct {
	FilesToCopy  []string `json:"filesToCopy"`
	UrisToDelete []string `json:"urisToDelete"`
}

// PostCommitManifestResponse (a.k.a. sendManifest) is the structure of the object returned from CommitManifest endpoint a.k.a. sendManifest
type PostCommitManifestResponse struct {
	Message     string 		 `json:"message"`
	Error       bool   		 `json:"error"`
	Transaction Transaction  `json:"transaction"`
}

// CommitManifest


// commit

// publish

// rollback
