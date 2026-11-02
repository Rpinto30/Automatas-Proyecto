package com.automatas5.process;

public class Automate{
    public static int GetHeightByChar(char c){

        switch (c) {
            case '#':
                return -5;
            case '@':
                return -4;
            case '%':
                    return -3;
            case '*':
                    return -2;
            case ';':
                    return -1;
            case ':':
                    return 0;
            case '!':
                    return 1;
            case '-':
                    return 2;
            case ',':
                    return 3;
            case '.':
                    return 4;
            case ' ':
                    return 5;
            default:
                return 0;
         
        }
    }

    /* 
    * @param force: between -1 and 1
    */ 
    public static char GetCharByForce(double force) {
        if (force < -1.0 || force > 1.0) return '\0';

        if (force >= 0.8) return ' ';  
        if (force >= 0.6) return '.'; 
        if (force >= 0.4) return ',';
        if (force >= 0.2) return '-';
        if (force >= 0.0) return '!'; 
        if (force >= -0.2) return ':'; 
        if (force >= -0.4) return ';';
        if (force >= -0.6) return '*'; 
        if (force >= -0.8) return '%'; 
        if (force >= -0.99) return '@';
        return '#';
    }


}