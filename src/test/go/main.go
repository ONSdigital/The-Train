package main

import (
	"context"
	"fmt"
	"github.com/ONSdigital/dp-the-train-feature-tests/config"
	"github.com/ONSdigital/log.go/v2/log"
	"os"
)

var (
	// BuildTime represents the time in which the service was built
	BuildTime string = "1601119818"
	// GitCommit represents the commit (SHA-1) hash of the service that is running
	GitCommit string = "TBD HERE"
	// Version represents the version of the service that is running
	Version   string = "v0.1.0"
)

func main() {
	ctx := context.Background()

	if err := run(ctx); err != nil {
		log.Fatal(ctx, "fatal runtime error", err)
		os.Exit(1)
	}
}

func run(ctx context.Context) error {
	// Read config
	cfg, err := config.Get()
	if err != nil {
		return fmt.Errorf("unable to retrieve service configuration: %w", err)
	}
	var transacPath string
	log.Info(ctx, "config on startup", log.Data{"config": cfg, "build_time": BuildTime, "git-commit": GitCommit})
	// Check if TransactionStore directory has been passed in as argument.
	if len(os.Args) > 1 {
		// Argument available so check it's a valid path on disk
		// Argument [1] is the path of the Go executable
		// Argument [2] is the first argument when running this procedure.
		fileInfo, err := os.Stat(os.Args[1])
		CheckError(err)
		if fileInfo.IsDir() {
			// Valid path for storing
			transacPath = os.Args[1]
		}
		transacPath = os.Args[1]
	} else {
		// No Env Variables
		con, err := config.Get()
		CheckErrorWithMsg(err, "failed getting env variable for transaction store")
		transacPath = con.TransactionStore
	}

	// Setup Tests
	CreateTransactionJSONs(transacPath)
	err = CopyHistoricTransactionsToStore(transacPath)
	CheckErrorWithMsg(err, "failed: copying older transactions to store.")

	// Make sure that context is cancelled when 'run' finishes its execution.
	// Any remaining go-routine that was not terminated during svc.Close (graceful shutdown) will be terminated by ctx.Done()
	var cancel context.CancelFunc
	ctx, cancel = context.WithCancel(ctx)
	defer cancel()
	return nil
}

func CheckError(err error) {
	ctx := context.Background()
	if err != nil {
		log.Fatal(ctx, "failed creating transaction.json file", err)
	}
}


func CheckErrorWithMsg(err error, msg string) {
	if err != nil {
		CheckError(fmt.Errorf("%v, %v", err, msg))
	}
}