package main

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/ONSdigital/log.go/v2/log"
)
 const historicTransactions = "/resources/transactions/"


 var transacStore string


// CopyHistoricTransactionsToStore will copy them from the resource folder to the appropriate location
func CopyHistoricTransactionsToStore (transacPath string) error {
	ctx := context.Background()
	path, err := os.Getwd()
	if err != nil {
		log.Fatal(ctx, "Failed checking current path", err)
	}
	// Copy the historic transactions to the Transaction Store
	err = CopyDirectory(filepath.Join(path, historicTransactions), transacPath)
	if err != nil {
		log.Fatal(ctx, "Failed: copying directories", err)
	}
	fmt.Println(path)
	return nil
}

// CreateTransactionJSONs creates Transactions with dates and times in the future for testing whether
// The-Train archival process works correctly.
func CreateTransactionJSONs(transacPath string){
	ctx := context.Background()
	futureTime := time.Now().Local().Add(time.Hour * 4)
	//
	// stringFT should resemble the following format:
	//                            2022-01-12T12:39:56.954+0000
	stringFT := fmt.Sprintf("%d-%02d-%02dT%02d:%02d:%02d.123+0000",
		futureTime.Year(), futureTime.Month(), futureTime.Day(),
		futureTime.Hour(), futureTime.Minute(), futureTime.Second())
	//
	// Transactions to be created
	//
	futureOK1 := Transaction{
		Id:         "futureOK1aaa0fae1670f12369368d141f3f68620697e81d49fef5e73c363db52e289b",
		Status:     "started",
		StartDate:  stringFT,
		EndDate:    "",
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     nil,
		Files:      nil,
	}

	futureOK2 := Transaction{
		Id:         "futureOK2aaa0fae1670f12369368d141f3f68620697e81d49fef5e73c363db52e289b",
		Status:     "started",
		StartDate:  stringFT,
		EndDate:    stringFT,
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     nil,
		Files:      nil,
	}

	futureErrors1 := Transaction{
		Id:         "futureErrors1aaae5809942d94ec6f0d8dd7bbe34cba5e23de873ceb470004a",
		Status:     "started",
		StartDate:  "", // Nil StartDate should throw an error
		EndDate:    stringFT,
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     []string{"no start", "date"},
		Files:      nil,
	}

	futureErrors2 := Transaction{
		Id:         "futureErrors2aaae5809942d94ec6f0d8dd7bbe34cba5e23de873ceb470004a",
		Status:     "started",
		StartDate:  "stoppedClockIs", // Erroneous time
		EndDate:    "correctTwiceADay",  // Erroneous time
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     []string{"erroneous start date", "erroneous end date"},
		Files:      nil,
	}

	Errors3 := Transaction{
		Id:         "Errors3aaa1670f12369368d141f3f68620697e81d49fef5e73c363db52e289b",
		Status:     "started",
		StartDate:  "error should occur",
		EndDate:    "",
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     nil,
		Files:      nil,
	}

	Errors4 := Transaction{
		Id:         "Errors4aaa1670f12369368d141f3f68620697e81d49fef5e73c363db52e289b",
		Status:     "started",
		StartDate:  "",
		EndDate:    "",
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     []string{"first of two errors", "second of two errors"},
		Files:      nil,
	}

	Errors5 := Transaction{
		Id:         "Errors5aaa1670f12369368d141f3f68620697e81d49fef5e73c363db52e289b",
		Status:     "statusNeverHeardOf",
		StartDate:  "",
		EndDate:    "",
		UriInfos:   nil,
		UriDeletes: nil,
		Errors:     []string{"first of two errors", "second of two errors"},
		Files:      nil,
	}

	transactions := []Transaction {futureOK1, futureOK2,
		futureErrors1, futureErrors2, Errors3, Errors4, Errors5}

	for _, t := range transactions {
		// Marshal to string
		serialised, err := json.Marshal(t)
		if err != nil {
			log.Fatal(ctx, "Failed: marshalling", err)
		}

		// Path for transaction.json
		jsonPath := filepath.Join(transacPath, t.Id)

		// Check path exists, if not create directories as necessary
		err = os.MkdirAll(jsonPath, 0755)
		if err != nil {
			log.Fatal(ctx, "Failed: making directory", err)
		}

		// Create the backup and content directories
		err = os.MkdirAll(filepath.Join(jsonPath,"backup"), 0755)
		if err != nil {
			log.Fatal(ctx, "Failed: making backup directory", err)
		}
		err = os.MkdirAll(filepath.Join(jsonPath,"content"), 0755)
		if err != nil {
			log.Fatal(ctx, "Failed: making content directory", err)
		}

		//Write transaction.json file
		err = os.WriteFile(filepath.Join(jsonPath, "transaction.json"), serialised, 0644)
		if err != nil {
			log.Fatal(ctx, "Failed: creating transaction.json file", err)
		}
	}
}

