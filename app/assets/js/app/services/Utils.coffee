"use strict"
ngDefine "services.Utils", (module) ->

  module.factory "Utils", () ->

    splitArrayIntoEqualChunks = (chunkCount, array) ->
      if(array.length == 0) then return []
      itemCountInEachColumn = Math.ceil(array.length / chunkCount)
      chunked = []
      for i in [0..chunkCount - 1]
        chunked.push(array.slice(i * itemCountInEachColumn, (if(i < chunkCount - 1) then ((i + 1) * itemCountInEachColumn) else undefined)))
      return chunked

    splitArrayIntoChunks = (chunkCount, array) ->
      innerCounter = 0
      innerArray = []
      chunked = []
      for i in [0..array.length - 1]
        if(innerCounter == chunkCount)
          chunked.push(innerArray)
          innerArray = []
          innerCounter = 0

        innerArray.push(array[i])
        innerCounter++

      if(innerCounter > 0) then chunked.push(innerArray)
      return chunked

    api = {
      splitArrayIntoEqualChunks: splitArrayIntoEqualChunks
      splitArrayIntoChunks: splitArrayIntoChunks
    }
    api