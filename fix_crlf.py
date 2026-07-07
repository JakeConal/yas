import sys

f = sys.argv[1]
with open(f, 'rb') as file:
    content = file.read()
content = content.replace(b'\r\n', b'\n')
with open(f, 'wb') as file:
    file.write(content)
