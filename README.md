# wwf_api - An API for Words With Friends

Christopher Mullins <http://christophermullins.net>

wwf_api is a Java library that enables communication with Zynga's popular
Scrabble variant, Words With Friends. It also provides some utility classes for
maintaining and manipulating game state.

## Disclaimer

This project is in no way associated with or endorsed by Zynga.

## Documentation

### Javadoc 

You can find the generated javadoc (which should be at least mildly helpful) here: 

<http://sidoh.github.com/wwf_api/javadoc>

### Thrift Definitions

This project uses [Thrift](http://thrift.apache.org) for many of its data
models. To quickly review any thrift types or service definitions, you can
consult the [thrift
definitions](https://github.com/sidoh/wwf_api/tree/master/src/main/thrift).

## Authentication

All operations that communicate with Zynga require an *accessToken*. As far as
I can tell, this is the oauth token assigned to Zynga by Facebook for an
authenticated user. wwf_api includes a small helper class that uses
[HtmlUnit](http://htmlunit.sourceforge.net/) to login to Facebook and extract
the oauth token from WWF page. This can be done as follows:

```java
AccessTokenRetriever tokenRetriever = new AccessTokenRetriever();
String accessToken = tokenRetriever.getAccessToken("foo@bar.com", "areallyneatpassword");

// This will prompt the user for their Facebook login credentials.
String accessToken2 = tokenRetriever.promptForAccessToken();
```

After retrieving an access token, you can use a `StatefulApiProvider`, which
will prevent you from having to enter the access token any time you want to make
an API call.

```java
StatefulApiProvider api = new StatefulApiProvider(accessToken);
api.createRandomGame();
```

## Usage

The API is consumable in two ways:

1. Using the `ApiProvider` class.
2. As a Thrift server, which allows languages other than Java to consume the
   service. 

The remainder of this section details operations supported by wwf_api and
assumes that you're using the `ApiProvider` class.

### Game Index

This allows you to get a list of all games that you've participated in
recently. The `GameIndex` object encapsulates the information returned by this
call.

```java
StatefulApiProvider api = new StatefulApiProvider(accessToken);
GameIndex index = api.getGameIndex();
GameMeta firstGame = index.getGames().get(0);
```

You'll notice that the list of `GameMeta` objects contain only metadata for
each game. To get the full `GameState` object, use the `getGameState` method.

### Game State

To access the game state for a particular `GameMeta` object, use the
`getGameState` method:

```java
StatefulApiProvider api = new StatefulApiProvider(accessToken);
GameIndex index = api.getGameIndex();
GameMeta firstGame = index.getGames().get(0);
GameState state = api.getGameState(firstGame.getGameId());
```

The `GameState` object contains the following information about a particular game:

1. A list of `MoveData` objects in chronological order.
2. The racks for *both* players as a map with the keys being the user IDs
   available in `GameMeta`. This is available because of the way that game state
   is maintained.
3. The current game board.
4. The scores for each player as a map keyed on user IDs.
5. The `GameMeta` object for the same game.
6. A list of `Tile`s remaining. These do not include the `Tile`s in the user's
   racks.
7. A list of `ChatMessage`objects sent during this game.

While you're more than welcome to build your own data models around what's
returned by the API, this project also includes some things to help you. For
example, you can use the `WordsWithFriendsBoard` class to easily access and
manipulate the game state:

```java
WordsWithFriendsBoard board = new WordsWithFriendsBoard(new BoardStorage(state.getBoard()));
Slot centerSlot = board.getSlot(7, 7);
Move.Result moveResult = board.move(move); // This updates the board
```

The return value of the `move` is a `Move.Result`, which contains some useful
information about the move that was placed. This includes:

1. The word that was formed by placing the tiles included in the corresponding
   `Move`.
2. The number of points earned by playing this word. This computation includes
   any relevant bonuses.
3. A list of all of the words formed by this play. This includes words formed by
   a parallel play.

### Making Moves

When submitting a move, you can either pass, resign, swap tiles, or make a
play. The API supports doing all of these. You can use the `GameStateHelper`
class to make your life easier in doing this.

##### To resign or pass
```java
GameStateHelper helper = new GameStateHelper();
GameState updatedState1 = api.makeMove(gameState1, helper.createMoveSubmission(MoveType.RESIGN));
GameState updatedState2 = api.makeMove(gameState2, helper.createMoveSubmission(MoveType.PASS));
```

##### To submit a play

The most straightforward way to do this is to construct a `Move` object. This
includes the following information:

1. A list of tiles that are actually played in the order that they're played.
   This should include *only* the tiles that are moved from the player's rack to
   the board.
2. The row and column that the first tile is placed in
3. A `WordOrientation` that specifies whether the play is vertical or
   horizontal (it can be either for one-letter plays).

Then, one can use the `GameStateHelper` to submit the move to the API.

```java
GameStateHelper helper = new GameStateHelper();
GameState updatedState = api.makeMove(gameState, helper.createMoveSubmissionFromPlay(move));
```

### Creating Games

Zynga has two methods for creating games:

1. Matchmaking -- you are assigned a random opponent also looking for a matchmaking game.
2. Invitation -- you invite someone you already know to play a game.

Creating a matchmaking game is easy. You just need the access token:

```java
api.createRandomGame();
```

Creating a game by invitation requires that you know either the facebook or
zynga ID of your desired opponent. The Zynga IDs are included in `GameMeta`
objects.

### Chatting

The library allows you to send and receive chat messages. Although `GameState`
includes a list of `ChatMessage` objects, there is also a method to retrieve
the unread `ChatMessage` objects for a given game ID.

##### Sending a chat message

```java
api.sendChatMessage(gameMeta.getId(), "Hello there!");
```

##### Reading chat messages
```java
List<ChatMessage> unreadChats = api.getUnseenChats(gameMeta.getId());

for (ChatMessage chatMessage : unreadChats) {
  System.out.printf("[%s] <%s> %s\n",
    chatMessage.getCreatedAt(),
    state.getMeta().getUsersById().get(chatMessage.getUserId()).getName(),
    chatMessage.getMessage());
}
```

### Dictionary Lookup

Before submitting a move, it's a good idea to verify that all of the words
formed by your move are actually in Zynga's dictionary. They use the enable1
word list, but they've removed some "offensive" words and added a few of their
own. You can poll their dictionary using the API.

The `dictionaryLookup` method accepts a set of strings (case-insensitive --
they'll be normalized). It returns a set of words that are *not* in the WWF
dictionary. If it returns an empty set, then all of your words are in the
dictionary!

```java
// This will NOT modify the board
Move.Result result = board.scoreMove(move);

if ( api.dictionaryLookup(result.getResultingWords()).size() > 0 ) {
  System.out.println("Uh oh, one of these words isn't in the dictionary!"); 
}
```

## Running the Thrift Server

If you'd like to consume this API in an environment that can't use a java
library, then you can run the [Thrift](http://thrift.apache.org/) server
defined in `ApiServer`. To build an executable jar and run the thrift server,
use the following:

```bash
mvn clean compile assembly:single
java -classpath $CLASSPATH:target/wwf_api-0.1-jar-with-dependencies.jar org.sidoh.wwf_api.ApiServer 1111
```

You can then consume the service on port 1111. The thrift definition files are
located in `./src/main/thrift`. If you'd like help setting up a thrift client
in the language of your choice, please contact me and I'll do  what I can.
