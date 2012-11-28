package Work {

import Chisel._
import Node._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class partialDeriv(windowSize: Integer, dataWidth: Integer, pdWidth: Integer) extends Component {
  val io = new Bundle {
    val din   = Vec(windowSize) { Fix(dir = INPUT, width = dataWidth) }
    val Ex = Fix(OUTPUT, pdWidth)
    val Ey = Fix(OUTPUT, pdWidth)
    val Et = Fix(OUTPUT, pdWidth)
  }

//im1(i,j+1)-im1(i,j)+im1(i+1,j+1)-im1(i+1,j);
  val Ex_1 = io.din(0)-io.din(1)+io.din(2)-io.din(3)
  val Ex_2 = io.din(4)-io.din(5)+io.din(6)-io.din(7)
  io.Ex := (Ex_1 + Ex_2) >>UFix(2)
//im1(i+1,j)-im1(i,j)+im1(i+1,j+1)-im1(i,j+1);
  val Ey_1 = io.din(0)-io.din(2)+io.din(1)-io.din(3)
  val Ey_2 = io.din(4)-io.din(6)+io.din(5)-io.din(7)
  io.Ey := (Ey_1 + Ey_2)>>UFix(2)
//
  val Et_1 = io.din(4)-io.din(0)+io.din(5)-io.din(1)
  val Et_2 = io.din(6)-io.din(2)+io.din(7)-io.din(3)
  io.Et := (Et_1 + Et_2)>>UFix(2)
  }

class partialDerivTest(c: partialDeriv) extends Tester(c, Array(c.io)) {
  defTests{
    var allGood = true
    val vars = new HashMap[Node, Node]()
    val rnd = new Random()
    val window = 8
    val wid = 16
    val maxInt = 255
    val inputs = new Array[Integer](window)
    def pderiv_x(data: Array[Integer]) = 
    {
      var Ex = data(1)-data(0)+data(3)-data(2)+data(5)-data(4)+data(7)-data(6)
      Ex = Ex/4
      Ex.toInt
    }
    
    def pderiv_y(data: Array[Integer]) = 
    {
      var Ey = data(2)-data(0)+data(3)-data(1)+data(6)-data(4)+data(7)-data(5)
      Ey = Ey/4
      Ey.toInt
    }

    def pderiv_t(data: Array[Integer]) = 
    {
      var Et = data(4)-data(0)+data(5)-data(1)+data(6)-data(2)+data(7)-data(3)
      Et = Et/4
      Et.toInt
    }
    for (cnt <- 0 until 20){
      for (i <- 0 until window){
        inputs(i) = rnd.nextInt(255)
        vars(c.io.din(i)) = UFix(inputs(i), wid)}
      vars(c.io.Ex) = UFix(pderiv_x(inputs))
      vars(c.io.Ey) = UFix(pderiv_y(inputs))
      vars(c.io.Et) = UFix(pderiv_t(inputs))
      allGood = step(vars) && allGood
    }
    allGood
  }
}
}
