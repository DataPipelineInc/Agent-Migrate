#!/bin/bash
TGT=com.datapipeline.agent.ConfigMigrationKt
ARG=""
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -h | --help)
    cat << EOF
Usage: start [options] [arguments] start parse oracle redo.
Arguments:
  -h, --help              give this help list.
  -a, --args              argument pass to program.
  -d, --debug [y|n]       debug mode, suspend = y(es) or n(o).
  -p, --progress          execute progress migration.
  -l, --data              execute data migration(last step).
Env:
  SRC_ID=[src id]
EOF
    exit 0
    ;;
    -a | --args)
    ARG="${ARG} $2"
    shift # pass argument
    shift # pass value
    ;;
    -d | --debug)
    DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=$2,address=5005
    shift # pass argument
    shift # pass value
    ;;
    -p | --progress)
    TGT=com.datapipeline.agent.ProgressMigrationKt
    shift
    ;;
    -l | --data)
    TGT=com.datapipeline.agent.DataMigrationKt
    shift
    ;;
    *)
    break
    ;;
esac
done

cmd="java -cp agent-migrate-1.0.2.jar $DEBUG_OPTS $TGT $ARG"
eval "$cmd"

