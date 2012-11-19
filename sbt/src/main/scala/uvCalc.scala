package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvCalc(pdWidth: Integer, pdFrac: Integer, dWidth: Integer, pWidth: Integer, dpFrac: Integer, uvWidth: Integer, uvFrac: Integer) extends Component {
  val io = new Bundle {
//    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val Ex = Fix(INPUT, pdWidth)
    val Ey = Fix(INPUT, pdWidth)
    val Et = Fix(INPUT, pdWidth)
    val P  = Fix(OUTPUT, pdWidth+uvWidth)
    val D  = Fix(OUTPUT, pdWidth+uvWidth)
    val uAvg = Fix(INPUT, uvWidth)
    val vAvg = Fix(INPUT, uvWidth)
    val u = Fix(OUTPUT, uvWidth)
    val v = Fix(OUTPUT, uvWidth)
  }
  val a = UFix(1<<dpFrac)
  io.P := io.Ex * io.uAvg + io.Ey * io.vAvg //+ io.Et<<UFix(uvFrac)
  io.D := io.Ex * io.Ex + io.Ey * io.Ey + a*a     //a*a
  val P_D = io.P/io.D //truncate 
  io.u := io.uAvg - io.Ex * P_D    // divide by D later
  io.v := io.vAvg - io.Ey * P_D    // divide by D later
  }

class uvCalcTest(c: uvCalc) extends Tester(c, Array(c.io)) {
  defTests{
    var allGood = true
    val vars = new HashMap[Node, Node]()
    val rnd = new Random()
    val window = 8
    val wid = 26 
    var a = 1 
    var inputs_Ex = 1
    var inputs_Ey = 1
    var inputs_Et = 1
    var inputs_uAvg = 1
    var inputs_vAvg = 1
    val maxInt =  33554432
    def uCalc(Ex: Integer, Ey: Integer, Et: Integer, uAvg: Integer, vAvg: Integer) = 
    {
      var P = Ex*uAvg + Ey*vAvg //+ Et
      var D = Ex^2 + Ey^2 + a^2 
      var u = uAvg - Ex*P/D
      P.toInt
    }
    def vCalc(Ex: Integer, Ey: Integer, Et: Integer, uAvg: Integer, vAvg: Integer) =
    {
      var P = Ex*uAvg + Ey*vAvg + Et
      var D = Ex^2 + Ey^2 + a^2 
      var v = vAvg - Ex*P/D
      D.toInt
    }
    for (cnt <- 0 until 1){
        inputs_Ex = rnd.nextInt(maxInt)
        inputs_Ey = rnd.nextInt(maxInt)
        inputs_Et = rnd.nextInt(maxInt)
        inputs_uAvg = rnd.nextInt(maxInt)
        inputs_vAvg = rnd.nextInt(maxInt)
        vars(c.io.Ex) = Fix(inputs_Ex, wid)
        vars(c.io.Ey) = Fix(inputs_Ey, wid)
        vars(c.io.Et) = Fix(inputs_Et, wid)
        vars(c.io.uAvg) = Fix(inputs_uAvg, wid)
        vars(c.io.vAvg) = Fix(inputs_vAvg, wid)
      vars(c.io.P) = Fix(uCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
      vars(c.io.D) = Fix(vCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
//      vars(c.io.u) = Fix(uCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
//      vars(c.io.v) = Fix(vCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
      allGood = step(vars) && allGood
    }
    allGood
  }
}
}
