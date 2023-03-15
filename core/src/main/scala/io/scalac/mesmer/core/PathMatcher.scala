package io.scalac.mesmer.core

sealed trait PathMatcher extends (String => Boolean) with Ordered[PathMatcher] {

  // must not contain trailing slash
  protected def base: String

  protected def priority: Int

  final def apply(path: String): Boolean = matches(path)

  def matches(path: String): Boolean

  def compare(that: PathMatcher): Int =
    if (sameBase(that)) {
      priority - that.priority
    } else if (startsWith(that)) {
      1
    } else if (that.startsWith(this)) {
      -1
    } else 0

  protected def startsWith(other: PathMatcher): Boolean = {
    val otherBase = if (other.base.endsWith("/")) other.base else s"${other.base}/"
    base.startsWith(otherBase)
  }

  protected def sameBase(other: PathMatcher): Boolean = {
    val otherBase = if (other.base.endsWith("/")) other.base else s"${other.base}/"
    val selfBase  = if (base.endsWith("/")) base else s"$base/"
    selfBase == otherBase
  }
}

object PathMatcher {

  // prefix match everything with a prefix
  private[core] final case class Prefix(base: String, withExactBase: Boolean) extends PathMatcher {
    protected val priority: Int = if (withExactBase) 3 else 0

    def matches(path: String): Boolean = {
      val baseTrailing = if (base.endsWith("/")) base else s"$base/"
      val pathTrailing = if (path.endsWith("/")) path else s"$path/"
      pathTrailing.startsWith(baseTrailing) && (withExactBase || pathTrailing.substring(baseTrailing.length).nonEmpty)
    }
  }

  private[core] final case class Exact(base: String) extends PathMatcher {

    protected val priority = 2

    def matches(path: String): Boolean = path == base || s"$path/" == base
  }

  private[core] final case class SingleVariable(base: String) extends PathMatcher {
    protected val priority = 1

    def matches(path: String): Boolean = {
      val trailingSlash = if (base.endsWith("/")) base else s"$base/"
      path.startsWith(trailingSlash) && !path.substring(trailingSlash.length).contains('/')
    }
  }

  sealed trait Error {
    def message: String
  }

  object Error {

    case object MissingSlashError extends Error {
      val message = "Path must start with a slash"
    }
  }

  def prefix(path: String, withExact: Boolean): Either[Error, PathMatcher] =
    withValidPath(path, Prefix(path, withExact))

  def singleVariable(path: String): Either[Error, PathMatcher] = withValidPath(path, SingleVariable(path))

  def exact(path: String): Either[Error, PathMatcher] = withValidPath(path, Exact(path))

  private def withValidPath(path: String, const: => PathMatcher): Either[Error, PathMatcher] = if (
    path.startsWith("/")
  ) {
    Right(const)
  } else Left(Error.MissingSlashError)

}
