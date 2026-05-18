public class RemQuo {

    // divide 13 by 7 and give remainder and quotient, but dont use div or %
    //logic : substract 7 from 13 until rem is < 7, the rem is remainder and no of times it got substracted is quotient.
     public static void main(String[] args) {
        
        int dividend = 13;
        int divisor = 7;
        int remainder = 0;
        int quotient = 0;
        while (divisor <= dividend) {
            remainder = dividend - divisor;
            dividend = remainder;
            quotient++;
        }

        System.out.println("Remainder : " + remainder);
        System.out.println("Quotient : " + quotient);

     }
}
