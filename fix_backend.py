file_path = r"c:\Users\andre\Downloads\Lector-PDF-CODEX-PR4\alturas-backend\src\main\java\com\backend\dto\employee\EmployeeRequest.java"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the duplicate package and duplicated class declaration
# The file right now has:
# package com.backend.dto.employee;
# 
# package com.backend.dto.employee;
# ...
# public class EmployeeRequest {
# 

import re
content = re.sub(r'package com\.backend\.dto\.employee;\s+package com\.backend\.dto\.employee;', 'package com.backend.dto.employee;', content)
content = re.sub(r'public class EmployeeRequest \{\s+public class EmployeeRequest \{', 'public class EmployeeRequest {', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("File fixed.")
