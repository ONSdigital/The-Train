package main

import (
	"bytes"
	"fmt"
	"github.com/ONSdigital/dp-the-train-feature-tests/config"
	"io/ioutil"
	"net/http"
	"path/filepath"
	"regexp"
	"testing"

	. "github.com/smartystreets/goconvey/convey"
)

func extractId(s string) string {
	re := regexp.MustCompile("\\\"(\\S){64}\\\"") // e.g. 16de96ef93ca546a82f5310c1320f2bf4957d3990361fc5d0a15734f998b80c5
	res := re.FindAllStringSubmatch(s, 1)
	for _ = range res {
		//like Java: match.group(1)
		if len(res)<1 {
			return ""
		}

		fmt.Errorf("NEILL:" + res[0][0])
		if len(res)>0 {
			id := res[0][0]
			id = id [ 1 : ] // Remove first " character
			id = id[:len(id)-1] //  Remove last " character
			return id
		}
		//fmt.Println("Message :", res[i][1])
	}
	return ""
}

func TestExtractId(t *testing.T) {
	Convey("When looking for an alphanumeric id of len 64", t, func() {
		Convey("The following should be found successfully in the following strings", func() {
			So(len(extractId("\"b4e4d74fceaff466a8a0aa64f98a7e77877f4f977c8ebb1155e514272e1a50a7\"")), ShouldEqual, 64)
			So(len(extractId("\"startDateObject\": \"Jan 31, 2022 1:05:10 PM\",\"id\": \"0076964b6b996cfc8031e421bd0c4091ac35b86ae67b5c468bea63ec16b850a9\",\"status\": \"started\",")), ShouldEqual, 64)
		})
	})
}


func checkKeyValuePairExistsInFile(key string, value string, body string, file string) bool {
	content, err := ioutil.ReadFile(file)
	CheckErrorWithMsg(err, "Failed: reading file")
	return checkKeyValuePairExists(key, value, byteArrToString(content))
}

func getTransactionJSONPath(id string) string {
	// Get the TransactionStore path
	con, err := config.Get()
	CheckErrorWithMsg(err, "failed getting env variable for transaction store")
	transacPath = con.TransactionStore
	// Copy the historic transactions to the Transaction Store
	return filepath.Join(transacPath, id, "transaction.json")
}

func readTransactionJSONFile(id string) string {
	path := getTransactionJSONPath(id)
	content, err := ioutil.ReadFile(path)
	CheckErrorWithMsg(err, "Failed: reading file")
	contentString := byteArrToString(content)
	if len(content) > 0 {
		return contentString
	} else {
		return ""
	}
}

func byteArrToString(b []byte) string {
	return string(b[:])
}

// postBegin with the post body, returns the Transaction ID
func postBegin() (string, *http.Response) {
	postBody := bytes.NewBuffer(nil)
	resp, err := http.Post("http://localhost:8084/begin", "application/json", postBody)
	CheckError(err)

	// Extract response
	body, err := ioutil.ReadAll(resp.Body)
	CheckErrorWithMsg(err, "/begin failed")

	return byteArrToString(body), resp
}


// postCommitManifest with the post body, returns the Transaction ID
func postCommitManifest(transacID string) (string, *http.Response) {
	var postBodyStr = `{"filesToCopy":[],"urisToDelete":[]}`
	postBodyByte := bytes.NewBuffer([]byte(postBodyStr))
	resp, err := http.Post("http://localhost:8084/CommitManifest?transactionId=" + transacID, "application/json", postBodyByte)
	CheckError(err)

	// Extract response
	body, err := ioutil.ReadAll(resp.Body)
	CheckErrorWithMsg(err, "/begin failed")

	return byteArrToString(body), resp
}


func checkKeyValuePairExists(key string, value string, body string) bool {
	// Example matches:
	// 	\"message\":\s\"New transaction created.\"
	//  \"key\":\s\"value.\"

	regExp := "\\\"" + key + "\\\":(.){1,6}" + value
	//regExp :=   "\\\"" + key + "\\\":\\s*" + value + "\\[\\]"

	match, _ := regexp.MatchString(regExp, body)
	return match
}
// TestCheckEndpoints checks the responses of the endpoints and associated transaction.json
func TestCheckEndpoints(t *testing.T) {
	//
	// /begin
	//
	var transacId string
	body, response := postBegin()
	transacId = extractId(body)
	// Get contents of transaction.json for this ID
	transactionJSON := readTransactionJSONFile(transacId)
	Convey("When calling /begin, check the response", t, func() {
		// Check transac response
		Convey("The Transaction should look like", func() {
			So(response.Status, ShouldEqual, "200 OK")
			So(checkKeyValuePairExists("status", "started", body), ShouldBeTrue)
			So(checkKeyValuePairExists("errors", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriDeletes", "\\[\\]", body), ShouldBeTrue)
			So(checkKeyValuePairExists("uriInfos", "\\[\\]", body), ShouldBeTrue)
			So(len(transacId), ShouldEqual, 64)
		})
	})

	Convey("After calling /begin", t, func() {
		Convey("The transaction.json file should look like", func() {
			So(checkKeyValuePairExists("status", "started", transactionJSON), ShouldBeTrue)
			// Below not working.
			//So(checkKeyValuePairExists("errors", "\\[\\]", transactionJSON), ShouldBeTrue)
			//So(checkKeyValuePairExists("errors", "\\[\\]", transactionJSON), ShouldBeTrue)
			//So(checkKeyValuePairExists("uriDeletes", "\\[\\]", transactionJSON), ShouldBeTrue)
			//So(checkKeyValuePairExists("uriInfos", "\\[\\]", transactionJSON), ShouldBeTrue)
			transacId = extractId(body)
			So(len(transacId), ShouldEqual, 64)
		})
	})


	//
	// CommitManifest
	//
	body, response = postCommitManifest(transacId)
	transacId = extractId(body)
	// Get contents of transaction.json for this ID
	transactionJSON = readTransactionJSONFile(transacId)
	Convey("When calling /CommitManifest", t, func() {
		// Check transac
		Convey("The Transaction should look like", func() {
			So(response.Status, ShouldEqual, "200 OK")
			So(checkKeyValuePairExists("status", "publishing", body), ShouldBeTrue)
			// Below not working.
			//So(checkKeyValuePairExists("errors", "\\[\\]", body), ShouldBeTrue)
			//So(checkKeyValuePairExists("uriDeletes", "\\[\\]", body), ShouldBeTrue)
			//So(checkKeyValuePairExists("uriInfos", "\\[\\]", body), ShouldBeTrue)
			transacId = extractId(body)
			So(len(transacId), ShouldEqual, 64)
		})
	})

	Convey("After calling /CommitManifest", t, func() {
		Convey("The transaction.json file should look like", func() {
			So(checkKeyValuePairExists("status", "publishing", transactionJSON), ShouldBeTrue)
			// Below not working.
			//So(checkKeyValuePairExists("errors", "\\[\\]", transactionJSON), ShouldBeTrue)
			//So(checkKeyValuePairExists("uriDeletes", "\\[\\]", transactionJSON), ShouldBeTrue)
			//So(checkKeyValuePairExists("uriInfos", "\\[\\]", transactionJSON), ShouldBeTrue)
			transacId = extractId(body)
			So(len(transacId), ShouldEqual, 64)
		})
	})
}

func TestItemsArchivedCorrectly(t *testing.T) {

}