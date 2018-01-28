package domain.model

sealed abstract class Market {
  val code: String
}
object Market {
  case object Tokyo extends Market {
    override val code: String = "TSE"
  }

  def fromCode(code: String) = code match {
    case Tokyo.code => Tokyo
  }
}