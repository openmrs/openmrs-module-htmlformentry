#!/bin/bash -eu

SRC_DIR=".."
SERVER_ID=""

function usage() {
  echo "USAGE:"
  echo "Watch webapp pages and resources from src and reflect them in the server"
  echo "Depends on the inotify-hookable command"
  echo ""
  echo "Input Options"
  echo " --srcDir : This should be the top-level dir where the module code is checked out.  Defaults to '..'"
  echo " --serverId : This is the name of the sdk server that you want the source code to be reflected into"
  echo " --help : prints this usage information"
  echo ""
  echo "Example"
  echo "  ./watch-resources.sh --srcDir=~/code/openmrs-module-htmlformentry --serverId=butaro2x"
  echo ""
  echo "If srcDir or serverId are not supplied, you will be prompted for these values"
}

# Input arguments are retrieved as options to the command
# This is the preferred way to invoke this without requiring user prompts, and only accepts artifact syntax
for i in "$@"
do
case $i in
    --srcDir=*)
      SRC_DIR="${i#*=}"
      shift # past argument=value
    ;;
    --serverId=*)
      SERVER_ID="${i#*=}"
      shift # past argument=value
    ;;
    --help)
      usage
      exit 0
    ;;
    *)
      usage    # unknown option
      exit 1
    ;;
esac
done

if [ -z "$SERVER_ID" ]; then
  read -e -p 'Server ID: ' -i "" SERVER_ID
fi

WEBAPP_DIR="$SRC_DIR/omod/src/main/webapp"
CP_CMD="cp -R $WEBAPP_DIR/* ~/openmrs/$SERVER_ID/tmp/openmrs/WEB-INF/view/module/htmlformentry/"
inotify-hookable -w $WEBAPP_DIR --recursive -c "$CP_CMD"
