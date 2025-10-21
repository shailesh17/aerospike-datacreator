# Architecture Overview

## Purpose

The Generic Data Creator is a Java application designed to generate and load large volumes of data into an Aerospike database. It can be used for performance testing, data modeling, and other development and operational tasks. The application also supports reading data from the database to verify data integrity and measure read performance.

## Core Components

The application is composed of several key components that work together to generate and load data:

*   **`GenericDataCreator`:** The main class that orchestrates the entire data generation process. It is responsible for:
    *   Reading the configuration from a JSON file.
    *   Initializing and managing `Writer` and `Reader` threads.
    *   Collecting and reporting metrics.
    *   Managing the application lifecycle.

*   **`Config`:** A Plain Old Java Object (POJO) that represents the application's configuration. It is populated from a JSON file and defines all the parameters for connecting to Aerospike, generating data, and controlling the read/write workload.

*   **`Writer`:** A runnable class responsible for writing data to the Aerospike database. Each `Writer` thread performs the following actions:
    *   Generates keys using a specified `KeyGenerator`.
    *   Creates records with randomly generated data based on a `RecordTemplate`.
    *   Writes the records to the Aerospike database.
    *   Optionally, passes the generated keys to a `KeyQueue` for consumption by `Reader` threads.

*   **`Reader`:** A runnable class responsible for reading data from the Aerospike database. Each `Reader` thread performs the following actions:
    *   Retrieves keys from the `KeyQueue` (if using a `PipedKeyGenerator`) or generates keys using its own `KeyGenerator`.
    *   Reads the corresponding records from the Aerospike database.

*   **`KeyGenerator`:** An interface for generating Aerospike keys. The application provides several implementations to support different key generation strategies:
    *   `SequentialKeyGenerator`: Generates sequential numeric keys.
    *   `RandomKeyGenerator`: Generates random numeric keys.
    *   `RandomStringKeyGenerator`: Generates random string keys.
    *   `PipedKeyGenerator`: Retrieves keys from the `KeyQueue`, allowing for a read-what-you-write pattern.

*   **`RecordTemplate`:** Defines the structure and content of the records to be generated. The template is specified in the JSON configuration file and allows for the creation of records with various data types, including strings, integers, longs, and binary data.

*   **`KeyQueue`:** A thread-safe queue used to pass keys from `Writer` threads to `Reader` threads. This is particularly useful for the `PipedKeyGenerator`, which enables `Reader` threads to read the same keys that were just written by the `Writer` threads.

*   **Metrics:** The application uses the Dropwizard Metrics library to collect and report a wide range of metrics, including:
    *   Write and read rates (transactions per second).
    *   Write and read latencies (minimum, maximum, and average).
    *   Network traffic (bytes sent and received).
    *   Read hit ratio.

## Data Flow

The data flow in the application is as follows:

1.  The `main` method in the `GenericDataCreator` class is the application's entry point.
2.  It parses the command-line arguments to get the path to the JSON configuration file.
3.  The configuration file is read and parsed into a `Config` object.
4.  Based on the configuration, the application creates and starts one or more `Writer` and `Reader` threads.
5.  **Writing Data:**
    *   Each `Writer` thread generates a key using its configured `KeyGenerator`.
    *   It then creates a record with random data according to the `RecordTemplate`.
    *   The record is written to the Aerospike database using the Aerospike Java client.
    *   If configured, the key is added to the `KeyQueue`.
6.  **Reading Data:**
    *   Each `Reader` thread retrieves a key from the `KeyQueue` or generates a key using its own `KeyGenerator`.
    *   It then reads the corresponding record from the Aerospike database.
7.  Throughout the process, the application collects and reports metrics to the console.
8.  The application continues to run for a duration specified in the configuration file, after which it gracefully shuts down all threads and reports the final summary of metrics.

## Configuration

The application is configured using a JSON file. The `Config.java` class defines the structure of this configuration. Key configuration options include:

*   **Aerospike Connection:**
    *   `host`: The hostname or IP address of the Aerospike cluster.
    *   `port`: The port number of the Aerospike cluster.
    *   `namespace`: The namespace to use for the data.
    *   `set`: The set to use for the data.
*   **Workload:**
    *   `threads`: The number of `Writer` and `Reader` threads to use.
    *   `duration`: The duration of the test in minutes.
*   **Read/Write Operations:**
    *   `writes`: An array of write operations to perform. Each write operation specifies the `KeyGenerator` to use, the `RecordTemplate` for the data, and other parameters.
    *   `reads`: An array of read operations to perform. Each read operation specifies the `KeyGenerator` to use and other parameters.

## Extensibility

The application is designed to be extensible in several ways:

*   **Custom `KeyGenerator`:** You can create your own key generation logic by implementing the `KeyGenerator` interface and specifying your custom class in the JSON configuration file.
*   **New Data Templates:** You can define new data structures and data types by modifying the `RecordTemplate` and the JSON configuration.
*   **Custom Reporting:** You can extend the application to report metrics to other systems, such as Graphite or InfluxDB, by leveraging the Dropwizard Metrics library.
*   **AWS Lambda:** The application includes dependencies for AWS Lambda, suggesting that it can be packaged and deployed as a serverless function for on-demand data generation tasks.
