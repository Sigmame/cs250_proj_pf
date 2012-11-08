package Work {

import Chisel._
import Node._

// for testbench
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class convolution_wrapper(windowSize : Integer, dataWidth: Integer, coeffWidth: Integer, coeffFract: Integer, pipeStages: Integer) extends Component {
  val io = new Bundle {
    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val coeff = Vec(windowSize) { UFix(dir = INPUT, width = coeffWidth) }
    val dout = UFix(OUTPUT, dataWidth)
  }

  // register inputs (won't be moved during retiming)
  val din_regs = Reg(io.din)
  val coeff_regs = Reg(io.coeff)

  // instantiate combinational convolution module
  val conv = new convolution(windowSize, dataWidth, coeffWidth, coeffFract)
  conv.io.din := din_regs
  conv.io.coeff := coeff_regs

  // register convolution module's output
  val result_reg  = Reg(conv.io.dout)

  // need to make sure things are named a certain way because
  // register names need to be specified during retiming:
  // dout_reg is connected to the output of wrapper module (won't be moved during retiming)
  // result_reg is connected to the output of convolution module (will be moved during retiming)
  // NOTE: registers that are moved during retiming will be renamed, with names like clk_r_*

  val dout_reg = Reg(UFix(width = dataWidth))
  if (pipeStages == 1) {
    dout_reg := conv.io.dout
  } else if (pipeStages == 2) {
    dout_reg := result_reg
  } else {
    // add additional registers between result_reg and dout_reg (will be moved during retiming)
    val dout_pipe = Vec(pipeStages-2) { Reg() { UFix(width = dataWidth) } }
    dout_pipe(0) := result_reg
    for (i <- 1 until pipeStages-2) {
      dout_pipe(i) := dout_pipe(i-1)
    }
    dout_reg := dout_pipe(pipeStages-3)
  } 

  io.dout := dout_reg    
}

class convolution(windowSize: Integer, dataWidth: Integer, coeffWidth: Integer, coeffFract: Integer) extends Component {
  val io = new Bundle {
    val din   = Vec(windowSize) { UFix(dir = INPUT, width = dataWidth) }
    val coeff = Vec(windowSize) { UFix(dir = INPUT, width = coeffWidth) }
    val dout = UFix(OUTPUT, dataWidth)
  }

  // baseline single cycle (pure combinatonal) implementation operate on UFix
  val wid = dataWidth + coeffWidth // expend width to avoid overflow
  val magn = Vec(windowSize) { UFix(width = wid)} // magnitude of multiplication
  val Mul = Vec(windowSize+1) { UFix(width = wid+1) } // intermediate multiplication results
  
  for (i <- 0 until windowSize) {
    magn(i) := io.coeff(i)*io.din(i) //UFix multiplication(???)
    Mul(i) := Cat(magn(i)(wid-1), magn(i)(wid-1, 0)).toUFix()
  }
  Mul(windowSize) := UFix(1 <<(coeffFract-1)) // round by adding 0.5

  // signed sum using UFix (2's complement) numbers
  val Sum = UFix(width = wid+1)
  Sum := foldR(Mul)(_+_) // summation resuls


  io.dout := Sum(coeffFract+dataWidth-1, coeffFract).toUFix() // truncate down to dataWidth
  when (Sum(wid) === Bits(1)) 
    {io.dout := UFix(0, dataWidth)} // negative values replaced by zeros
  .elsewhen (orR(Sum(wid-1, coeffFract+dataWidth)) === Bits(1))
    {io.dout := UFix(1 <<dataWidth) - UFix(1)} // positive values capped at maxInt
}

class convolutionTest(c: convolution) extends Tester(c, Array(c.io)) { //binds the tester to convolution and test its io
  defTests {
    var allGood = true // test result boolean: true->pass
    val vars    = new HashMap[Node, Node]() // mapping of test nodes to literals
    val rnd     = new Random() // random number generator
    val maxInt  = 255 // 4-bit wide integer
    val wid     = 8 //data bit width
    val window  = 25 //window size
    var cnt     = 0 // test counts
    val inputs  = new Array[Integer](window) // 3-by-3 test window
    val scale   = 115.0
//    val coeffs  = Array(2/scale,4/scale,5/scale,4/scale,2/scale, 4/scale,9/scale,12/scale,9/scale,4/scale, 5/scale,12/scale,15/scale,12/scale,5/scale, 4/scale,9/scale,12/scale,9/scale,4/scale, 2/scale,4/scale,5/scale,4/scale,2/scale) // gaussian
    val coeffs  = Array(-4.0,-1.0,0.0,-1.0,-4.0, -1.0,2.0,3.0,2.0,-1.0, 0.0,3.0,4.0,3.0,0.0, -1.0,2.0,3.0,2.0,-1.0, -4.0,-1.0,0.0,-1.0,-4.0) // laplace

    def convolve(co: Array[Double], data: Array[Integer])  =
    {
      var sum = 0.5
      for (i <- 0 until window) {
        sum += data(i)*co(i)
      }
      if (sum < 0) sum = 0
      if (sum > maxInt) sum = maxInt
      sum.toInt
    } // get the convolution of the input

    var pos = 0.0
    for (cnt <- 0 until 20) { // run tests
      for (i <- 0 until window) {
        inputs(i) = rnd.nextInt(maxInt/8)
        vars(c.io.din(i)) = UFix(inputs(i), wid)
        if (coeffs(i) >= 0) {vars(c.io.coeff(i)) = UFix((coeffs(i)*4096).toInt, width = 16)} // gaussian & laplace
        else {
          pos = 65536 + coeffs(i)*4096
          vars(c.io.coeff(i)) = UFix(pos.toInt, width = 16)
        }
      } // generate random input window
      vars(c.io.dout) = UFix(convolve(coeffs, inputs))
      allGood = step(vars) && allGood // steps the DUT and compare to expected values in vars
    }
    allGood // all TRUE -> pass
  }
}

}
