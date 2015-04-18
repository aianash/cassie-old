package cassie.service

import scaldi.Module
import scaldi.Injectable._

package object injectors {
  class CassieServiceModule extends Module {
    bind [CassieService] to new CassieService
  }
}