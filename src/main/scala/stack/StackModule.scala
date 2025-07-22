package stack

import chisel3._
import chisel3.stage.ChiselStage
import java.nio.file.Paths

// Your code starts here
import chisel3.util._

class StackModule(val dataWidth: Int, val len: Int) extends Module {
  val io = IO(new Bundle {

  
    val in = Input(UInt(32.W))

    // Output for popped/peeked values
    val out = Output(UInt(dataWidth.W))

    // Status flags
    val underflow = Output(Bool())
    val overflow = Output(Bool())
    val isEmpty = Output(Bool())
    val isFull = Output(Bool())
    val popped = Output(Bool())
    val peeked = Output(Bool())
  })

  
  val PUSH_OP = "b0100111".U(7.W)
  val POP_OP  = "b1000011".U(7.W)
  val PEEK_OP = "b1000000".U(7.W)

  val opcode = io.in(6, 0)
  val immediate = io.in(31, 7) // imm[24:0]

  // state elements
  val stackMem = Reg(Vec(len, UInt(dataWidth.W)))
  val sp = RegInit(0.U(log2Ceil(len + 1).W))

  //  creating registers for all event outputs
  val outReg = RegInit(0.U(dataWidth.W))
  val underflowReg = RegInit(false.B)
  val overflowReg = RegInit(false.B)
  val poppedReg = RegInit(false.B)
  val peekedReg = RegInit(false.B)

  //connect registers to the output ports
  io.out := outReg
  io.underflow := underflowReg
  io.overflow := overflowReg
  io.popped := poppedReg
  io.peeked := peekedReg

  // status logic 
  io.isEmpty := (sp === 0.U)
  io.isFull := (sp === len.U)
  
  //  set default values for the next clock cycle
  // They will be changed again by the instruction logic if an event occurs.
  outReg := 0.U // default output is 0 unless a POP/PEEK happens
  underflowReg := false.B
  overflowReg := false.B
  poppedReg := false.B
  peekedReg := false.B

  // logic updates the registers, not the i/0 wires directly.
  switch(opcode) {
    is(PUSH_OP) {
      when(io.isFull) {
        overflowReg := true.B
      } .otherwise {
        stackMem(sp) := immediate
        sp := sp + 1.U
      }
    }

    is(POP_OP) {
      when(io.isEmpty) {
        underflowReg := true.B
        outReg := 0.U //  0 on underflow
      } .otherwise {
        sp := sp - 1.U
        outReg := stackMem(sp - 1.U)
        poppedReg := true.B
      }
    }

    is(PEEK_OP) {
      when(io.isEmpty) {
        underflowReg := true.B
        outReg := 0.U // 0 on underflow
      } .otherwise {
        outReg := stackMem(sp - 1.U)
        peekedReg := true.B
      }
    }
  }
}

// Your code ends here

object SVGen extends App {
  val out = Paths.get(
    "out",
    this.getClass
      .getName
      .stripSuffix("$")
  ).toString
  new ChiselStage().emitSystemVerilog(
    new StackModule(args(0).toInt, args(1).toInt),
    Array("--target-dir", out),
  )
}
