guidelines:

Reading of files inside current directory and it's subdirectories is allowed.
This is clojure project. All my dependencies are linked inside checkouts
subdirectory, feel free to read them if needed ( do not ask me for permission ). 
All other dependencies are open source, use internet to understand them if 
needed. For reading of all other files ask for permission.

issues:

CLAUDE-1 draw-text-center
Add method draw-text-center in clj-common.2d. It should be copy of draw-text
with font. Text should be rendered centred horizontally based on image-context
width and font's metrics for text width

CLAUDE-2 text-justify
Implement text-justify in clj-common.2d. It should accept length and text. It 
should return text of length given as arument. In case text ends with "." add
whitespaces at the end. Otherwise add white spaces where possible.

CLAUDE-3 draw-lines-center
Using draw-text-center add new method which will write sequence of lines.
Starting at given y and then incrementing ( using half of font height as
margin. Argument name should be line-seq.
