import time
import board
import busio
import neopixel
import adafruit_drv2605
import adafruit_ble
import adafruit_led_animation.color as color
from adafruit_ble.advertising.standard import ProvideServicesAdvertisement
from adafruit_ble.services.standard.device_info import DeviceInfoService
from adafruit_ble.services.nordic import UARTService
from adafruit_bluefruit_connect.accelerometer_packet import AccelerometerPacket
from adafruit_bluefruit_connect.packet import Packet
from digitalio import DigitalInOut, Direction

# setup for onboard NeoPixel
pixel_pin = board.NEOPIXEL
num_pixels = 1

pixel = neopixel.NeoPixel(pixel_pin, num_pixels, brightness=0.1, auto_write=False)

# setup for haptic motor driver
i2c = busio.I2C(board.SCL, board.SDA)
drv = adafruit_drv2605.DRV2605(i2c)

# onboard blue LED
blue_led = DigitalInOut(board.BLUE_LED)
blue_led.direction = Direction.OUTPUT

# setup for BLE
ble = adafruit_ble.BLERadio()
if ble.connected:
    for c in ble.connections:
        c.disconnect()

advertisement = ProvideServicesAdvertisement()

# add device info service and UART service for BLE to advertise
device_info_service = DeviceInfoService(manufacturer="CHDS")
uart_service = UARTService()
advertisement.services.append(device_info_service)
advertisement.services.append(uart_service)

# function for haptic motor vibration
# num: # of times to vibrate
# duration: duration of vibration
# delay: time between vibrations
def vibrate(num, duration, delay):
    # 16 is the vibration effect being used for the haptic motor
    drv.sequence[0] = adafruit_drv2605.Effect(16)
    for _ in range(0, num):
        drv.play()  # start vibration
        time.sleep(duration)
        drv.stop()  # stop vibration
        time.sleep(delay)

while True:
    # start BLE
    ble.start_advertising(advertisement)
    blue_led.value = False
    pixel.fill(color.RED)
    pixel.show()
    print("Waiting for connection")

    # NeoPixel is red when not connected to BLE
    while not ble.connected:
        blue_led.value = False

    # blue LED is on when connected
    blue_led.value = True
    pixel.fill(color.BLUE)
    pixel.show()
    print("Connected")

    for connection in ble.connections:
        if connection.connected:
            if not connection.paired:
                #  pairs to phone
                connection.pair(bond=True)
                print("paired")

    while ble.connected:
        if uart_service.in_waiting:
            line = uart_service.readline()
            command = line.strip().decode('utf-8')
            print(command)

            if command == 'alert':
                pixel.fill(color.CYAN)
                pixel.show()
                vibrate(2, 2, 1)

            pixel.fill(color.BLUE)
            pixel.show()

            uart_service.write(line)

    # if BLE becomes disconnected then blue LED turns off
    # and BLE begins advertising again to reconnect
    blue_led.value = False
    print("Disconnected")
    print()
