"use strict"
ngDefine "filters.cut", (module) ->
  # Reference: http://stackoverflow.com/a/18096071/158523
  module.filter 'cut', () ->
    (value, max, wordwise, tail) ->
      if (!value) then return ''

      max = parseInt(max, 10)
      if (!max?) then return value
      if (value.length <= max) then return value

      value = value.substr(0, max)
      if (wordwise)
        lastSpace = value.lastIndexOf(' ')
        if (lastSpace != -1)
          value = value.substr(0, lastSpace)

      return value + (tail || ' …')