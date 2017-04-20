#include<xc.h>           // processor SFR definitions
#include<sys/attribs.h>  // __ISR macro
#include<stdio.h>
#include "ILI9163C.h"
#include "i2c_master_noint.h"

// DEVCFG0
#pragma config DEBUG = OFF // no debugging
#pragma config JTAGEN = OFF // no jtag
#pragma config ICESEL = ICS_PGx1 // use PGED1 and PGEC1
#pragma config PWP = OFF // no write protect
#pragma config BWP = OFF // no boot write protect
#pragma config CP = OFF // no code protect

// DEVCFG1
#pragma config FNOSC = FRCPLL // use primary oscillator with pll
#pragma config FSOSCEN = OFF // turn off secondary oscillator
#pragma config IESO = OFF // no switching clocks
#pragma config POSCMOD = HS // high speed crystal mode
#pragma config OSCIOFNC = OFF // disable secondary osc
#pragma config FPBDIV = DIV_1 // divide sysclk freq by 1 for peripheral bus clock
#pragma config FCKSM = CSDCMD // do not enable clock switch
#pragma config WDTPS = PS1048576 // use slowest wdt
#pragma config WINDIS = OFF // wdt no window mode
#pragma config FWDTEN = OFF // wdt disabled
#pragma config FWDTWINSZ = WINSZ_25 // wdt window at 25%

// DEVCFG2 - get the sysclk clock to 48MHz from the 8MHz crystal
#pragma config FPLLIDIV = DIV_2 // divide input clock to be in range 4-5MHz
#pragma config FPLLMUL = MUL_24 // multiply clock after FPLLIDIV
#pragma config FPLLODIV = DIV_2 // divide clock after FPLLMUL to get 48MHz
#pragma config UPLLIDIV = DIV_2 // divider for the 8MHz input clock, then multiplied by 12 to get 48MHz for USB
#pragma config UPLLEN = ON // USB clock on

// DEVCFG3
#pragma config USERID = 00011010 // (26) some 16bit userid, doesn't matter what
#pragma config PMDL1WAY = OFF // allow multiple reconfigurations
#pragma config IOL1WAY = OFF // allow multiple reconfigurations
#pragma config FUSBIDIO = ON // USB pins controlled by USB module
#pragma config FVBUSONIO = ON // USB BUSON controlled by USB module

// Definitions
#define CLOCK 48000000
#define BCKGRND BLUE             // Background LCD color is "BLUE"   (0x001F)
#define TEXT WHITE               // Text LCD color is "WHITE"        (0xFFFF)
#define ACCEL_ADDR 0b11010100    // Address of LSM6DS33 (0 = default write)

void I2C2_init(void);            // I2C2 initialization function

int main() {
    __builtin_disable_interrupts();

    // set the CP0 CONFIG register to indicate that kseg0 is cacheable (0x3)
    __builtin_mtc0(_CP0_CONFIG, _CP0_CONFIG_SELECT, 0xa4210583);
    
    // 0 data RAM access wait states
    BMXCONbits.BMXWSDRM = 0x0;

    // enable multi vector interrupts
    INTCONbits.MVEC = 0x1;

    // disable JTAG to get pins back
    DDPCONbits.JTAGEN = 0;
    
    I2C2_init();    // initializes I2C2 peripheral
    SPI1_init();    // initializes SPI1 peripheral
    LCD_init();     // initializes LCD screen

    
    __builtin_enable_interrupts();
    
    LCD_clearScreen(BCKGRND);   // sets LCD screen to color BCKGRND
    
    while(1)    {

    }
}

void I2C2_init(void)    {
    ANSELBbits.ANSB2 = 0;           // turn of analog on B2 and B3
    ANSELBbits.ANSB3 = 0;           // SDA2 (B2) and SCL2 (B3))
    
    i2c2_master_setup();            // turns on I2C
    
    i2c2_master_start();
    i2c2_master_send(ACCEL_ADDR);
    i2c2_master_send(0x10);         // access CTRL1_XL register
    i2c2_master_send();
    i2c2_master_stop();
    
    i2c2_master_start();
    i2c2_master_send(ACCEL_ADDR);
    i2c2_master_send(0x11);         // access CTRL2_G register
    i2c2_master_send();
    i2c2_master_stop();
    
    i2c2_master_start();
    i2c2_master_send(ACCEL_ADDR);
    i2c2_master_send(0x12);         // access CTRL3_C register
    i2c2_master_send();
    i2c2_master_stop();
}