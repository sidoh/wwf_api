#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR/../../..

tmphtml=$(mktemp -t index.html.XXXXX)
echo "<!DOCTYPE html?>
<html>
  <head>
    <title>wwf_api Javadoc</title>
  </head>
  <body>
    <h1> wwf_api javadoc index </h1>
    <hr/>
    <ol>" > $tmphtml

for tag in $(git tag); do
  echo "<li><a href=\"javadoc/$tag\">$tag</a></li>" >> $tmphtml
done

echo "</ol></body></html>" >> $tmphtml
git checkout gh-pages
git pull
rm -rf javadoc/HEAD

for tag in $(echo $(git tag) HEAD); do
  if [ ! -e javadoc/$tag ]; then
    git checkout $tag

    working_dir=$(mktemp -d -t javadoc.XXXXXXX)
    javadoc -d $working_dir -sourcepath src/main/java -subpackages org.sidoh -exclude ec.util:org.sidoh.wwf_api.types

    git checkout gh-pages
    mv $working_dir javadoc/$tag
    git add javadoc
    git commit -am "update javadoc for $tag"
  fi
done
