# Inventory Sorter

Simple inventory sorting for Minecraft

## Features

Scroll wheel
> moves a single item from one inventory to another.

Middle mouse sorting
> Click the middle mouse button to sort the inventory from highest to least count. Uses item registry name to group similarly named items when the count is the same.

## Details
The mod requires installation on both client and server to function. It makes no attempt to sort in the client, rather delegating all work to the server. This means it is very reliable and very fast. It should work with all containers (and maybe too many), and tries to respect canExtract/isItemValid (it will avoid slots that are marked that way).

## License
This project is licensed according to GPL v3, see gpl-3.0.txt or https://www.gnu.org/licenses/gpl-3.0.en.html for more information.