package Work

import Chisel._
  
object Work {
  def main(args: Array[String]): Unit = {
    val mainArgs = args.slice(2, args.length)
    val res =
    args(0) match {
      case "filter" =>
        chiselMain( mainArgs, () => 
            new convolutionFilter(maxImageWidth = 1024,
                                  maxImageHeight = 1024,
                                  dataWidth = 8, 
                                  coeffWidth = 16,  // coefficient width (including sign bit)
                                  coeffFract = 12,   // number of bits to right of binary point
                                  pipeStages = args(1).toInt))
      
      case "convolution5x5test" => 
        chiselMainTest( mainArgs, () => 
            new convolution(windowSize = 25,
                            dataWidth = 8, 
                            coeffWidth = 16, 
                            coeffFract = 12)){ c => new convolutionTest(c) }
      
      case "windowBuf5x5test" => 
        chiselMainTest( mainArgs, () => 
            new windowBuf5x5(maxImageWidth = 1024,
                             dataWidth = 8)){ c => new windowBufTest(c) }

    }
  } 
}   

