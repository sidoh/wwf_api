#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

javadoc -d $DIR/../../../gh-pages/javadoc -sourcepath src/main/java -subpackages org.sidoh -exclude ec.util:org.sidoh.wwf_api.types
