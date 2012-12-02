extern void initialize_image_buffers(input reg [31:0] width, input reg [31:0] height);
extern void generate_input_image();
extern void generate_output_image();
extern void get_input_pixel  (input reg [31:0] offset, output reg [31:0] dout_1, output reg [31:0] dout_2);
extern void get_output_pixel (input reg [31:0] offset, output reg [31:0] dout_u, output reg [31:0] dout_v);

`define IMAGE_WIDTH 512
`define IMAGE_HEIGHT 256
`define IMAGE_SIZE `IMAGE_WIDTH * `IMAGE_HEIGHT
`define FP_WIDTH 26

module hsOptFlowTopTestHarness_rtl;

  // clock signal
  reg clk = 0;
  always #`CLOCK_PERIOD clk = ~clk;

  // reset signal
  reg  reset = 1'b1;
  reg  reset_ext = 1'b1;
  always @(posedge clk)
    reset <= reset_ext;

  wire #`INPUT_DELAY reset_in = reset;

  // instantiate and connect hsOptFlow module
  reg frame_sync_in_reg = 1'b0;
  wire #`INPUT_DELAY frame_sync_in = frame_sync_in_reg;

  reg [7:0] data_in1_reg = 8'd0;
  reg [7:0] data_in2_reg = 8'd0;
  wire [7:0] #`INPUT_DELAY data_in1 = data_in1_reg;
  wire [7:0] #`INPUT_DELAY data_in2 = data_in2_reg;

  wire [`FP_WIDTH-1:0] data_out_u;
  wire [`FP_WIDTH-1:0] data_out_v;
  wire frame_sync_out;

  hsOptFlowTop hsOptFlow_dut
  ( 
    .clk              (clk),
    .reset            (reset_in),

//    .io_image_width   (10'd`IMAGE_WIDTH-1'b1),
//    .io_image_height  (10'd`IMAGE_HEIGHT-1'b1),

    .io_frame_sync_in     (frame_sync_in),
    .io_data_in1          (data_in1),
    .io_data_in2          (data_in2),

    .io_frame_sync_out    (frame_sync_out),
    .io_data_out_u        (data_out_u),
    .io_data_out_v        (data_out_v)
  );

  // get command line options, initialize simulation
  reg [31:0] max_cycles;
  reg [31:0] num_images;
  reg [31:0] vcdpluson;

  initial
  begin

    // Get number of test images to try before finishing
    if (!$value$plusargs("num-images=%d", num_images))
      num_images = 1;

    // Get max number of cycles to run simulation for from command line
    // defaults to number neccessary to compute specified number of test
    // images
    if (!$value$plusargs("max-cycles=%d", max_cycles))
      max_cycles = (num_images * `IMAGE_SIZE) + (`IMAGE_WIDTH*10);

    // turn on VPD trace file generation
    if ($value$plusargs("vcdpluson=%d", vcdpluson))
      if (vcdpluson != 0) 
        $vcdpluson(0);

    // initialize testbench
    initialize_image_buffers(`IMAGE_WIDTH, `IMAGE_HEIGHT);

    // enable warnings about comparisons with X's or Z's
    $xzcheckon;

    // Strobe reset
    reset_ext = 1;
    #4 reset_ext = 0;

  end

  // cycle counter, completion and timeout checks
  reg [31:0] cycle_count = 32'd0;
  reg done = 1'b0;
  reg failed = 1'b0;
  always @(posedge clk)
  begin
    cycle_count <= cycle_count + 1;

    if (done)
    begin
      if (failed)
        $display("*** FAILED *** exiting after %d cycles", cycle_count);
      else
        $display("*** PASSED *** exiting after %d cycles", cycle_count);
      $finish;
    end

    if (cycle_count % 1000 == 0)
      $display("Cycle: %d", cycle_count);

    if (cycle_count > max_cycles)
    begin
      $display("*** FAILED *** (timeout)");
      $finish;
   end
  end

  // generate input test vectors and verify design output
  // using DirectC function calls to get input and expected output
  // pixel values
  reg started = 1'b0;
  reg image_failed = 1'b0;
  reg mismatch = 1'b0;
  reg [31:0] input_offset = 32'd0;
  reg [31:0] output_offset = 32'd0;
  reg [31:0] data_in32_1;
  reg [31:0] data_in32_2;
  reg [31:0] correct_dout_u = 32'd0;
  reg [31:0] correct_dout_v = 32'd0;
  reg [31:0] image_count = 32'd0;

  always @(posedge clk)
  begin
    if (cycle_count >= 4)
    begin
      if (input_offset == 0)
      begin
        // generate a new random test input image
        generate_input_image();
        frame_sync_in_reg <= 1'b1;
      end
      else
        frame_sync_in_reg <= 1'b0;

      // get value of input image at specified offset
      get_input_pixel(input_offset, data_in32_1, data_in32_2);
      data_in1_reg <= data_in32_1[7:0];
      data_in2_reg <= data_in32_2[7:0];

      if (input_offset == `IMAGE_SIZE-1)
        input_offset <= 0;
      else
        input_offset <= input_offset + 1;

      if (frame_sync_out && !started)
      begin
        // filter input image to produce expected output image
        generate_output_image();
        started <= 1'b1;
      end

      if (started || frame_sync_out)
      begin
        // get value of expected output image at specified offset
        get_output_pixel(output_offset, correct_dout_u, correct_dout_v);
        if (data_out_u == correct_dout_u[`FP_WIDTH-1:0])
          mismatch <= 1'b0;
        else
        begin
          $display("ERROR: Mismatch at cycle %d : expected %02x : actual %02x", cycle_count, correct_dout_u[`FP_WIDTH-1:0], data_out_u);
          failed <= 1'b1;
          image_failed <= 1'b1;
          mismatch <= 1'b1;
        end

        if (output_offset == `IMAGE_SIZE-1)
        begin
          if (image_failed)
            $display("[FAILED] Filter output for input image pair %d was incorrect!", image_count+1);
          else
            $display("[PASSED] Image pair %d processed succesfully.", image_count+1);

          image_failed <= 1'b0;

          if (image_count == num_images-1)
              done <= 1'b1;

          image_count <= image_count + 1;
          output_offset <= 0;

        end
        else
          output_offset <= output_offset + 1;
      end
    end
  end

endmodule
