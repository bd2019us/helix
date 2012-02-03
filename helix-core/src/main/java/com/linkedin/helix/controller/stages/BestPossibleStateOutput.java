package com.linkedin.helix.controller.stages;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.helix.model.ResourceKey;

public class BestPossibleStateOutput
{
  Map<String, Map<ResourceKey, Map<String, String>>> _dataMap;

  public BestPossibleStateOutput()
  {
    _dataMap = new HashMap<String, Map<ResourceKey, Map<String, String>>>();
  }

  public void setState(String resourceGroupName, ResourceKey resource,
      Map<String, String> bestInstanceStateMappingForResource)
  {
    if (!_dataMap.containsKey(resourceGroupName))
    {
      _dataMap.put(resourceGroupName,
          new HashMap<ResourceKey, Map<String, String>>());
    }
    Map<ResourceKey, Map<String, String>> map = _dataMap.get(resourceGroupName);
    map.put(resource, bestInstanceStateMappingForResource);
  }

  public Map<String, String> getInstanceStateMap(String resourceGroupName,
      ResourceKey resource)
  {
    Map<ResourceKey, Map<String, String>> map = _dataMap.get(resourceGroupName);
    if (map != null)
    {
      return map.get(resource);
    }
    return Collections.emptyMap();
  }

  public Map<ResourceKey, Map<String, String>> getResourceGroupMap(String resourceGroupName)
  {
    Map<ResourceKey, Map<String, String>> map = _dataMap.get(resourceGroupName);
    if (map != null)
    {
      return map;
    }
    return Collections.emptyMap();
  }

  @Override
  public String toString()
  {
    return _dataMap.toString();
  }
}