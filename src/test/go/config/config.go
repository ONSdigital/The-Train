package config

import (
	"encoding/json"
	"time"

	"github.com/kelseyhightower/envconfig"
)

// Config represents service configuration for the-train feature tests
type Config struct {
	BindAddr                   string        `envconfig:"BIND_ADDR"`
	GracefulShutdownTimeout    time.Duration `envconfig:"GRACEFUL_SHUTDOWN_TIMEOUT"`
	TransactionStore		   string		 `envconfig:"TRANSACTION_STORE"`
	ArchivingTransactionsPath  string		 `envconfig:"ARCHIVING_TRANSACTIONS_PATH"`
}

var cfg *Config

// Get returns the default config with any modifications through environment
func Get() (*Config, error) {
	if cfg != nil {
		return cfg, nil
	}

	cfg = &Config{
		BindAddr:                   "localhost:",
		GracefulShutdownTimeout:    5 * time.Second,
	}

	// overwrites default values with system env variable values
	return cfg, envconfig.Process("", cfg)
}

// String is implemented to prevent sensitive fields being logged.
// The config is returned as JSON with sensitive fields omitted.
func (config Config) String() string {
	b, _ := json.Marshal(config)
	return string(b)
}