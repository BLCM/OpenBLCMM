These are just some notes that I'd compiled while investigating the
various compile-to-EXE options available for Java apps.  There's a lot
of software out there that'll do it!  I did clean this up a bit after
the fact.  Spoiler: I ended up going with Liberica NIK for the Windows
EXE compilation.  See [`README-developing.md`](README-developing.md) for
info on building w/ Liberica.

# Stuff What Might Work

### [GraalVM](https://www.graalvm.org/22.2/reference-manual/native-image/)

Official Oracle thing, which is maybe not the greatest.  A paid version is
available, but the Community version would theoretically work well enough.
The main issue is that it currently [doesn't support Swing/AWT
applications on Windows](https://github.com/oracle/graal/issues/3084).
There's some info available on [doing so in Linux, though](https://www.praj.in/posts/2021/compiling-swing-apps-ahead-of-time/),
and some info on [the "tracing agent" in general](https://medium.com/graalvm/introducing-the-tracing-agent-simplifying-graalvm-native-image-configuration-c3b56c486271).
Doesn't do cross-compiling, though some CI/build-action stuff could be
configured to do so in the clown: [see here, for
instance](https://blogs.oracle.com/developers/post/building-cross-platform-native-images-with-graalvm).
Regardless, I did verify that Swing/AWT is currently a no-go (as of early
April, 2023), which brings us instead to:

### [Liberica Native Image Kit](https://bell-sw.com/liberica-native-image-kit/)

This is a fork of GraalVM which *does* support Swing/AWT.  It's got the
exact same building process -- ie: using a "tracing agent" to suss out JNI
calls and the like, and then passing those generated configs into the
compilation process.  It works great!  The tracing thing feels fiddly because
we could certainly miss out on some things (which would manifest in user
crashes once they hit the edge cases) but I feel good enough about it that
I'm moving forward with Liberica.

### [launch4j](https://launch4j.sourceforge.net/docs.html)

Long legacy of working fine, would have to figure out exactly what it
can do.  Supports configuring some memory parameters via an INI file,
so we may need to figure out editing that as well.  This is what BLCMM
currently uses.  In the end, the only way to reliably allow the user
to edit memory parameters would be to continue having a separate launcher
app, since the INI file is bound to an EXE, and I don't want someone
to make their app unlaunchable by putting in silly values (ie: the
launcher should be able to recover on its own without requiring the user
to delete/fix an INI file).  So honestly not super ideal, IMO.

### [jpackage](https://openjdk.org/jeps/392)

No cross-compiling, might not support Java 8?  Though I guess given
the nature of the thing, that might not be important.  Didn't really
look into this one so much since I'd looked at GraalVM/Liberica first,
and that ended up working out great.

# Stuff What Might Work But Is Paid, So Eff That

### [Jar2Exe](https://www.jar2exe.com/)

Paid.  Bleh.

### [JWrapper](https://www.jwrapper.com/)

Very much paid; clearly a business-oriented product

### [install4j](https://www.ej-technologies.com/products/install4j/overview.html)

Looks great, but very much paid as well.  Another business-oriented thing.

# Stuff What Either Won't Work Or Is Too Unmaintained

### [JSmooth](https://jsmooth.sourceforge.net/)

Ancient, hasn't been updated since 2007

### [jlink](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jlink.html)

Not actually what I'm looking for at all.  It basically strips down the JVM
into a custom build that only supports what the app needs, so you can then
distribute a smaller binary when shipping the JVM along with the app.  Might
be handy in some cases, of course, but not what I'm looking for at the moment.

