package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvCalc(pdWidth: Integer) extends Component {
  val io = new Bundle {
//    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val Ex = Fix(INPUT, pdWidth)
    val Ey = Fix(INPUT, pdWidth)
    val Et = Fix(INPUT, pdWidth)
    val uAvg = Fix(INPUT, pdWidth)
    val vAvg = Fix(INPUT, pdWidth)
    val u = Fix(OUTPUT, pdWidth)
    val v = Fix(OUTPUT, pdWidth)
  }
  val P = io.Ex * io.uAvg + io.Ey * io.vAvg + io.Et
  val D = io.Ex * io.Ex + io.Ey * io.Ey      //a*a
  io.u := io.uAvg - io.Ex * P    // divide by D later
  io.v := io.vAvg - io.Ey * P    // divide by D later
  }

class uvCalcTest(c: uvCalc) extends Tester(c, Array(c.io)) {
  defTests{
    var allGood = true
    val vars = new HashMap[Node, Node]()
    val rnd = new Random()
    val window = 8
    val wid = 16
    var inputs_Ex = 1
    var inputs_Ey = 1
    var inputs_Et = 1
    var inputs_uAvg = 1
    var inputs_vAvg = 1
    val maxInt = 255
    def uvCalc(Ex: Integer, Ey: Integer, Et: Integer, uAvg: Integer, vAvg: Integer) = 
    {
      var P = Ex*uAvg + Ey*vAvg + Et
      var D = Ex^2 + Ey^2 
      var u = uAvg - Ex*P/D
      var v = vAvg - Ey*P/D
      u.toInt
      v.toInt
    }
    for (cnt <- 0 until 20){
        inputs_Ex = rnd.nextInt(255)
        inputs_Ey = rnd.nextInt(255)
        inputs_Et = rnd.nextInt(255)
        inputs_uAvg = rnd.nextInt(255)
        inputs_vAvg = rnd.nextInt(255)
        vars(c.io.Ex) = UFix(inputs_Ex, wid)
        vars(c.io.Ey) = UFix(inputs_Ey, wid)
        vars(c.io.Et) = UFix(inputs_Et, wid)
        vars(c.io.uAvg) = UFix(inputs_uAvg, wid)
        vars(c.io.vAvg) = UFix(inputs_vAvg, wid)
      vars(c.io.u) = UFix(uvCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
      vars(c.io.v) = UFix(uvCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
      allGood = step(vars) && allGood
    }
    allGood
  }
}
}
