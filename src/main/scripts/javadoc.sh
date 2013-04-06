#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
working_dir=$(mktemp -d -t javadoc.XXXXXXX)

javadoc -d $working_dir -sourcepath src/main/java -subpackages org.sidoh -exclude ec.util:org.sidoh.wwf_api.types
git checkout --orphan gh-pages
git rm -rf *
echo '<a href="/wwf_api/javadoc">Javadoc</a>' > index.html
git add index.html
mv $working_dir javadoc
git add javadoc
git commit -am "update javadoc"
git push origin gh-pages
git checkout master
