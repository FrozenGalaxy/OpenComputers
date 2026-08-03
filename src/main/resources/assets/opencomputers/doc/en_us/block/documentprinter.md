# Document Printer

![Paperwork, but programmable.](item:opencomputers:document_printer)

The Document Printer is the OpenPrinter peripheral integrated into OpenComputers. Connect it to an OpenComputers network and access it as the `openprinter` component.

Load a [black or color ink cartridge](../item/printerink.md) into the left slots and paper into the paper input. The scanner slot accepts [printed pages](../item/printedpage.md) and vanilla books. Completed jobs appear in the output tray.

The printer uses a persistent FIFO queue. Jobs pause automatically when paper, ink, energy, or output space runs out and resume after the problem is corrected.

## Modern API

The preferred API queues complete documents and returns a job ID:

```lua
local printer = require("component").openprinter

local job, reason = printer.print("Line 1\nLine 2", {
  title = "Report",
  copies = 2,
  wrap = true
})
assert(job, reason)
```

Structured documents support per-line color and alignment:

```lua
printer.print({
  title = "Status",
  lines = {
    {text = "OpenComputers", color = 0x3366FF, alignment = "center"},
    {text = "Ready", color = 0x000000, alignment = "left"}
  }
})
```

Useful callbacks include:

* `print(document[, options])` — queue text or a structured document.
* `printLabel(text[, options])` — print name tags.
* `printMap(image[, options])` — print an RGB pixel table onto an empty map.
* `printImage(data[, options])` — decode PNG, JPEG, GIF, or BMP data and print it as a map.
* `status([jobId])`, `queue()`, `cancel(jobId)` — inspect and manage queued jobs.
* `scan()` — scan a printed page or vanilla book into a document that can be passed back to `print`.
* `supplies()` — report paper, ink, and output capacity.
* `capabilities()` — report API version, limits, and supported features.

Printer state changes emit an `openprinter_job` signal containing the printer address, job ID, state, and reason.

## Classic OpenPrinter API

Programs written for the original OpenPrinter can still use the buffered API:

```lua
local printer = require("component").openprinter

printer.setTitle("Classic Page")
printer.writeln("Hello, world!")
printer.writeln("In color", 0x3366FF, "center")
printer.print()
```

Compatibility callbacks include `writeln`, `setTitle`, `clear`, `print([copies])`, `getPaperLevel`, `getBlackInkLevel`, `getColorInkLevel`, `charCount`, `width`, `maxWidth`, `scanLine`, and `scanBook`.

The modern document API is recommended for new programs because it supports pagination, queues, status reporting, map/image printing, and automatic job resumption.

## OpenPrinter Tools Disk

The green **OpenPrinter** program disk includes command-line tools for OpenOS:

* `print`
* `printerstatus`
* `xerox`
* `printercopypage`
* `printmap`

Install the disk with OpenOS's normal installer.
