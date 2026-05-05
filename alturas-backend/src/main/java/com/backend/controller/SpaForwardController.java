package com.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {

  // Forward a index.html para rutas del SPA (evita /api)
  @RequestMapping(value = {
      "/{path:^(?!api$).*$}",
      "/{path:^(?!api$).*$}/**"
  })
  public String forward() {
    return "forward:/index.html";
  }
}
