#pragma once
// Test-only HAL: capture the real kernel's GPIO calls without an ESP32.
#define OUTPUT 1
#define HIGH 1
#define LOW 0
void pinMode(int pin,int mode);
void digitalWrite(int pin,int level);
