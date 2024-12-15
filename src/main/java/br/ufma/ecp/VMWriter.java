package br.ufma.ecp;

public class VMWriter {
    private StringBuilder vmOutput = new StringBuilder();

    enum Segment {
        CONST("constant"),
        ARG("argument"),
        LOCAL("local"),
        STATIC("static"),
        THIS("this"),
        THAT("that"),
        POINTER("pointer"),
        TEMP("temp");
    
        private final String value; 
    
        private Segment(String value) {
            this.value = value;
        }
    
        public String getValue() { 
            return value;
        }
    };

    enum Command {
        ADD,
        SUB,
        NEG,
        EQ,
        GT,
        LT,
        AND,
        OR,
        NOT
    };

    public String vmOutput() {
        return vmOutput.toString();
    }

    void writePush(Segment segment, int index) {
        vmOutput.append(String.format("push %s %d\n", segment.getValue(), index));
    }

    void writePop(Segment segment, int index) {

        vmOutput.append(String.format("pop %s %d\n", segment.getValue(), index));
    }

    void writeFunction(String name, int nLocals) {
        vmOutput.append(String.format("function %s %d\n", name, nLocals));
    }

    void writeCall(String name, int nArgs) {
        vmOutput.append(String.format("call %s %d\n", name, nArgs));
    }

}
