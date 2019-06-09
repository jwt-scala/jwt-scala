## JwtClaim Class

```tut
import java.time.Clock
import pdi.jwt.JwtClaim

JwtClaim()

implicit val clock: Clock = Clock.systemUTC

// Specify the content as JSON string
// (don't use var in your code if possible, this is just to ease the sample)
var claim = JwtClaim("""{"user":1}""")

// Append new content
claim = claim + """{"key1":"value1"}"""
claim = claim + ("key2", true)
claim = claim ++ (("key3", 3), ("key4", Seq(1, 2)), ("key5", ("key5.1", "Subkey")))

// Stringify as JSON
claim.toJson

// Manipulate basic attributes
// Set the issuer
claim = claim.by("Me")

// Set the audience
claim = claim.to("You")

// Set the subject
claim = claim.about("Something")

// Set the id
claim = claim.withId("42")

// Set the expiration
// In 10 seconds from now
claim = claim.expiresIn(5)
// At a specific timestamp (in seconds)
claim.expiresAt(1431520421)
// Right now! (the token is directly invalid...)
claim.expiresNow

// Set the beginning of the token (aka the "not before" attribute)
// 5 seconds ago
claim.startsIn(-5)
// At a specific timestamp (in seconds)
claim.startsAt(1431520421)
// Right now!
claim = claim.startsNow

// Set the date when the token was created
// (you should always use claim.issuedNow, but I let you do otherwise if needed)
// 5 seconds ago
claim.issuedIn(-5)
// At a specific timestamp (in seconds)
claim.issuedAt(1431520421)
// Right now!
claim = claim.issuedNow

// We can test if the claim is valid => testing if the current time is between "not before" and "expiration"
claim.isValid

// Also test the issuer and audience
claim.isValid("Me", "You")

// Let's stringify the final version
claim.toJson
```
