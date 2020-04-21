# Quarkus 1.3.1 - On the difference between the IO thread and a worker thread: a microbenchmark story

This repo contains the source code, results and scripts used for the quarkus blog post `On the difference between the IO thread and a worker thread: a microbenchmark story`

Docker is used to create the environment available to the System Under Test

We use [wrk2](https://github.com/giltene/wrk2) to drive the load from the client machine to the server running the System Under Test.  The understand why we ues wrk2, please read http://highscalability.com/blog/2015/10/5/your-load-generator-is-probably-lying-to-you-take-the-red-pi.html  

Running the benchmark is managed by a qDup script.  qDup is an automation tool that provides a way to coordinate multiple terminal shell connections for queuing performance tests and collecting output files  

Result parsing is provided by a custom jbang script

Timing of system startup and results graphing is provided by node.js scripts

## Results 

## Running the benchmark

### Pre-requsites

 - Docker
 - [sdkman](https://sdkman.io/)
 - [node.js](https://nodejs.org/en/)
 - [qDup](https://github.com/Hyperfoil/qDup/releases/tag/release-0.4.1)
 - [wrk2](https://github.com/giltene/wrk2)

### Setup

1. Ensure Docker Deamon is running on the server that you wish to run the applications. Please refer to Docker installation documentation for your particular system.

2. Install [node.js](https://nodejs.org/en/) on the server that will be used to run the benchmark applications.

3. Install [sdkman](https://sdkman.io/) on the server  that will be used to run the benchmark applications.

4. Ensure that you are able to open a remote SSH connection to the client and server machines from your current machine, without the need to enter a password.

    You can do this by adding your public ssh key to `~/.ssh/authorized_keys` on the client and server machines

5. Modify the following lines `scripts/qDup/benchmark.yaml` to point to your client and server machines

    ```yaml
   ...
    hosts:
      client: {USER}@{CLIENT_HOST}:22
      server: {USER}@{SERVER_HOST}:22
   ...
     TEST_ENDPOINT : http://{SERVER_HOST}:8080/hello/Bob
     ENVIRONMENT_URL: http://{SERVER_HOST}:8080/environment
   ...
    ``` 
    
    where; 
     - `{USER}` is the username you wish to connect to the remote machine with
     - `{CLIENT_HOST}` is the fully qualified domain name of the client machine to run generate load
     - `{SERVER_HOST}` is the fully qualified domain name of the server machine with the docker deamon already running in step (1)

6. Run the benchmark script with qDup: `java -jar {path_to_qDup}/qDup-0.4.1-uber.jar -B ./results/data ./scripts/qDup/benchmark.yaml`. 
    
    N.B. this script may appear to freeze, it takes approx 30 mins   to run and will not always write output to the terminal.

7. After the run has complete, process the run data with `processResults.sh`

    ```shell script
    $ ./processResults.sh 4 {CLIENT_HOST} {SERVER_HOST}
    ```   
   
   where;
    - `4` is the number of cpus (this is used to calculate the % cpu utilization)
    - `{CLIENT_HOST}` is the full qualified hostname of the client machine defined in  `scripts/qDup/benchmark.yaml` in step (5)
    - `{SERVER_HOST}` is the fully qualified domain name of the server machine defined in  `scripts/qDup/benchmark.yaml` in step (5)

8. Results and graphs will be available in `./results/runResult.json` and `./results/graphs/` respectively.

### Test Environment

## System Under Test

### Server

| benchserver4 |  |
| --- | --- |
| Hardware | HP ProLiant DL380 Gen9
| CPU's |  2 x Intel(R) Xeon(R) CPU E5-2640 v3 @ 2.60GHz| 
|  | 8 cores per socket, +HT, 32 cores total |
| Memory | 256GB |
| Storage | 2 x SanDisk ioDrive2 (600GB) - 2.4GB total |
|OS | Red Hat Enterprise Linux Server release 7.7 (Maipo) |
| Kernel | Linux benchserver4.perf.lab.eng.rdu2.redhat.com 3.10.0-1062.1.1.el7.x86_64 #1 SMP Tue Aug 13 18:39:59 UTC 2019 x86_64 x86_64 x86_64 GNU/Linux |

### Client

| benchclient1 |  |
| --- | --- |
| Hardware | HP ProLiant DL380 G7
| CPU's |  2 x Intel(R) Xeon(R) CPU X5660  @ 2.80GHz 
|  | 6 cores per socket, +HT, 24 cores total |
| Memory | 64GB |
| Storage | 4 x 146GB 15K RAID |
|OS | Red Hat Enterprise Linux Server release 7.6 (Maipo) |
| Kernel | Linux benchclient1 3.10.0-229.el7.x86_64 #1 SMP Thu Jan 29 18:37:38 EST 2015 x86_64 x86_64 x86_64 GNU/LinuxLinux benchclient1 3.10.0-229.el7.x86_64 #1 SMP Thu Jan 29 18:37:38 EST 2015 x86_64 x86_64 x86_64 GNU/Linux |
