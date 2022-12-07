#!/bin/bash
/home/winterwell/config/build-scripts/builder.sh \
BUILD_TYPE="production" \
PROJECT_NAME="moneyscript" \
NAME_OF_SERVICE="moneyscript" \
GIT_REPO_URL="github.com:/good-loop/moneyscript" \
PROJECT_ROOT_ON_SERVER="/home/winterwell/moneyscript" \
PROJECT_USES_BOB="yes" \
PROJECT_USES_NPM="yes" \
PROJECT_USES_WEBPACK="yes" \
PROJECT_USES_JERBIL="no" \
PROJECT_USES_WWAPPBASE_SYMLINK="yes"
