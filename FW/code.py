import board
import neopixel
import adafruit_ble
import adafruit_led_animation.color as color
from adafruit_ble.advertising.standard import ProvideServicesAdvertisement
from adafruit_ble.services.standard.device_info import DeviceInfoService
from adafruit_ble.services.nordic import UARTService
from adafruit_bluefruit_connect.accelerometer_packet import AccelerometerPacket
from adafruit_bluefruit_connect.packet import Packet
from digitalio import DigitalInOut, Direction

#  setup for onboard NeoPixel
pixel_pin = board.NEOPIXEL
num_pixels = 1

pixel = neopixel.NeoPixel(pixel_pin, num_pixels, brightness=0.1, auto_write=False)

#  onboard blue LED
blue_led = DigitalInOut(board.BLUE_LED)
blue_led.direction = Direction.OUTPUT

#  setup for BLE
ble = adafruit_ble.BLERadio()
if ble.connected:
    for c in ble.connections:
        c.disconnect()

advertisement = ProvideServicesAdvertisement()

device_info_service = DeviceInfoService(manufacturer="CHDS")
uart_service = UARTService()
advertisement.services.append(device_info_service)
advertisement.services.append(uart_service)

while True:
    ble.start_advertising(advertisement)
    blue_led.value = False
    print("Waiting for connection")

    #  NeoPixel is red when not connected to BLE
    while not ble.connected:
        blue_led.value = False
        pixel.fill(color.RED)
        pixel.show()
    print("Connected")

    pixel.fill(color.BLACK)
    pixel.show()

    while ble.connected:
        blue_led.value = True #  blue LED is on when connected

        # for connection in ble.connections:
        #     if not connection.paired:
        #         #  pairs to phone
        #         connection.pair()
        #         print("paired")

        if uart_service.in_waiting:
            line = uart_service.readline()
            print(line.decode('utf-8'))
            pixel.fill(color.CYAN)
            pixel.show()
            uart_service.write(line)

        pixel.fill(color.BLACK)
        pixel.show()

    #  if BLE becomes disconnected then blue LED turns off
    #  and BLE begins advertising again to reconnect
    print("Disconnected")
    blue_led.value = False
    print()
