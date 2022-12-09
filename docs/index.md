---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: home
---

![Logo]({{ '/assets/img/banner.png' | relative_url  }})
{: .home-banner }
<br>
A flexible, [open-source]({{ site.github-url }}) Android app for interval imaging and wildlife monitoring.
{: style="text-align: center;"}

<div id="carousel">
    {% for index in (1..7) %}
        <img src="{{ '/assets/img/carousel/' | relative_url }}{{ index }}.png"/>
    {% endfor %}
</div>