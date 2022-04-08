# img.ly - Android Trial Day Task

The goal of this task is to create a drawing application in native Android, which allows a user to annotate images by brushing on them. 
Feel free to use good old Java or Kotlin.

## Layout
### The layout doesn't have to look good, but it should be usable.

We require a very simple layout here and you can use an XML layout or code to create it:

  * Load Image -> Load an image from the gallery and display it on screen.
  * Save Image -> Save the final image to the gallery.
  * Color -> Let the user select a color from a list of colors.
  * Size -> Let the user specify the radius of a stroke via a slider.
  * Clear all -> Let the user remove all strokes from the image to start over.

# Requirements 

  * Your drawing must be applied to the original image, not the downscaled preview. That means that your brush must work in image coordinates instead of view coordinates and the saved image must have the same resolution as the originally loaded image.
  * You can ignore OutOfMemory issues. If you run into memory issues just use a smaller image for testing.

# Things to consider

These features would be nice to have. Structure your program in such a way, that it could be added afterwards easily. If you have time left, feel free to implement it already.

  * The user can make mistakes, so a history (undo/redo) would be nice.
  * The image usually doesn't change while brushing, but can be replaced with a higher resolution variant. A common scenario would be a small preview but a high-resolution final rendering. Keep this in mind when creating your data structures.
