DataPorts - A Data Platform for the Connection of Cognitive Ports https://dataports-project.eu/
On Demand Component

This project has received funding from the European Union's Horizon 2020 
research and innovation programme under grant agreement No 871493.

Copyright 2022 Universitat Politècnica de València


I. Used Software

This product uses the following software with corresponding licenses:

<#function licenseFormat licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#assign result = result + license/>
        <#if license_has_next>
            <#assign result = result + " or "/>
        </#if>
    </#list>
    <#return result>
</#function>
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    </#if>
</#function>
<#if dependencyMap?size == 0>
The project has no dependencies.
<#else>
    <#list dependencyMap as e>
        <#assign project = e.getKey()/>
        <#assign licenses = e.getValue()/>
        ${artifactFormat(project)} distributed under ${licenseFormat(licenses)}
    </#list>
</#if>