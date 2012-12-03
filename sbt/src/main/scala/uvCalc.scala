package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class uvCalc(doutWidth: Integer, fractWidth: Integer) extends Component {
  val io = new Bundle {
    val Ex = Fix(INPUT, doutWidth)
    val Ey = Fix(INPUT, doutWidth)
    val Et = Fix(INPUT, doutWidth)
    val P  = Fix(OUTPUT, 36)
    val D  = Fix(OUTPUT, 32)
    val uAvg = Fix(INPUT, doutWidth)
    val vAvg = Fix(INPUT, doutWidth)
    val u = Fix(OUTPUT, doutWidth)
    val v = Fix(OUTPUT, doutWidth)
  }
  val a = UFix(1<< fractWidth)
  val P_pre = io.Ex * io.uAvg + io.Ey * io.vAvg + (io.Et << UFix(fractWidth)) //Frac: 16+16
  val D_pre = io.Ex * io.Ex + io.Ey * io.Ey + a*a     //Fract: 16+16
  io.P := P_pre >> UFix(fractWidth) //Fract: 16
  io.D := D_pre >> UFix(fractWidth) 
  val Px = io.Ex * io.P 
  val Py = io.Ey * io.P  // Fract: 16+16
  io.u := io.uAvg - Px / io.D      //Fract: 32-16
  io.v := io.vAvg - Py / io.D   
  }

class uvCalcTest(c: uvCalc) extends Tester(c, Array(c.io)) {
  defTests{
    var allGood = true
    val vars = new HashMap[Node, Node]()
    val rnd = new Random()
    val window = 8
    val wid = 26 
    var a = 1<<14 
    var inputs_Ex = 1
    var inputs_Ey = 1
    var inputs_Et = 1
    var inputs_uAvg = 1
    var inputs_vAvg = 1
    val maxInt = 64
    val uvFrac = 16
    def uCalc(Ex: Integer, Ey: Integer, Et: Integer, uAvg: Integer, vAvg: Integer) = 
    {
      var P = Ex*uAvg + Ey*vAvg + Et<<uvFrac
      var D = Ex*Ex + Ey*Ey + a*a 
      var u = uAvg - Ex*P/D
      u.toInt
    }
    def vCalc(Ex: Integer, Ey: Integer, Et: Integer, uAvg: Integer, vAvg: Integer) =
    {
      var P = Ex*uAvg + Ey*vAvg + Et
      var D = Ex*Ex + Ey*Ey + a*a 
      var v = vAvg - Ex*P/D
      v.toInt
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
      vars(c.io.u) = Fix(uCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
      vars(c.io.v) = Fix(vCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
//      vars(c.io.u) = Fix(uCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
//      vars(c.io.v) = Fix(vCalc(inputs_Ex, inputs_Ey, inputs_Et, inputs_uAvg, inputs_vAvg))
      allGood = step(vars) && allGood
    }
    allGood
  }
}
}
